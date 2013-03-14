package lemur.nopol.encdet;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;

import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.StartTagType;
import net.htmlparser.jericho.Tag;

public class EncDetUils {

    /**
     * @return
     * @see Source#getCharsetParameterFromHttpHeaderValue
     */
    public static String getCharsetParameterFromHttpHeaderValue(
            final String httpHeaderValue) {
        final int charsetParameterPos = httpHeaderValue.toLowerCase().indexOf("charset=");
        if (charsetParameterPos == -1)
            return null;
        final int charsetBegin = charsetParameterPos + 8;
        int charsetEnd = httpHeaderValue.indexOf(';', charsetBegin);
        final String charset = (charsetEnd == -1) ? httpHeaderValue
                .substring(charsetBegin) : httpHeaderValue.substring(charsetBegin,
                charsetEnd);
        return charset.trim();
    }

    public static boolean isEncodingSupported(String encoding) {
        try {
            return Charset.isSupported(encoding);
        } catch (IllegalCharsetNameException ex) {
            return false;
        }
    }

    /**
     * Returns the encoding from the value of the HTTP header 'content type'.
     * 
     * @param contentType
     * 
     * @return The name of the encoding or null if it is not valid.
     * @see StreamEncodingDetector#StreamEncodingDetector
     */
    public static String encodingFromHttpHeader(String contentType) {
        String encoding = getCharsetParameterFromHttpHeaderValue(contentType);
        boolean encodingSupported = false;
        if (encoding != null && encoding.length() > 0) {
            try {
                if (Charset.isSupported(encoding))
                    encodingSupported = true;
            } catch (IllegalCharsetNameException ex) {
                if (encoding.charAt(0) == '"') {
                    String encodingWithoutQuotes = encoding.replace("\"", "");
                    if (isEncodingSupported(encodingWithoutQuotes)) {
                        // logger.warn("Encoding "+encoding+" specified in HTTP header is illegaly delimited with double quotes, which have been ignored");
                        encodingSupported = true;
                    } else {
                        // logger.warn("Encoding "+encoding+" specified in HTTP header is illegaly delimited with double quotes");
                    }
                    encoding = encodingWithoutQuotes;
                }
            }
            // logger.warn("Encoding "+encoding+" specified in HTTP header is not supported, attempting other means of detection");
        }

        return (encodingSupported) ? encoding : null;
    }

    public static String getDocumentSpecifiedEncoding(Source source,
            EncodingDetector encodingDetector) {
        final String UNINITIALISED = "";
        String documentSpecifiedEncoding = source.getDocumentSpecifiedEncoding();
        if (documentSpecifiedEncoding != UNINITIALISED)
            return documentSpecifiedEncoding;
        final Tag xmlDeclarationTag = source.getTagAt(0);
        if (xmlDeclarationTag != null
                && xmlDeclarationTag.getTagType() == StartTagType.XML_DECLARATION) {
            documentSpecifiedEncoding = ((StartTag) xmlDeclarationTag)
                    .getAttributeValue("encoding");
            if (documentSpecifiedEncoding != null)
                return documentSpecifiedEncoding;
        }
        // Check meta tags:
        for (StartTag metaTag : source.getAllStartTags(HTMLElementName.META)) {
            documentSpecifiedEncoding = metaTag.getAttributeValue("charset");
            if (documentSpecifiedEncoding == null) {
                if (!"content-type".equalsIgnoreCase(metaTag
                        .getAttributeValue("http-equiv")))
                    continue;
                final String contentValue = metaTag.getAttributeValue("content");
                if (contentValue == null)
                    continue;
                documentSpecifiedEncoding = getCharsetParameterFromHttpHeaderValue(contentValue);
                if (encodingDetector != null
                        && encodingDetector
                                .isIncompatibleWithPreliminaryEncoding(documentSpecifiedEncoding))
                    continue;
            }
            if (documentSpecifiedEncoding != null)
                return documentSpecifiedEncoding;
        }
        return null;
    }

}
