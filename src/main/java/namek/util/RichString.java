package namek.util;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.Arrays;

public class RichString {
  private static final String ABBREVIATION = "...";

  public static String join(String separator, Object... objects) {
    StringBuilder sb = new StringBuilder();
    sb.append(separator);
    for (Object object : objects) {
      String text = "";
      if (object != null) {
        text = StringUtils.trimToEmpty(object.toString());
      }
      sb.append(text).append(separator);
    }
    return sb.toString();
  }

  public static String abbreviateMiddle(String content, int maxSize) {
    return StringUtils.abbreviateMiddle(content, ABBREVIATION, maxSize);
  }

  public static String abbreviateUsingBytes(String content, int maxBytes) {
    if (content == null) {
      return null;
    }
    if (content.getBytes().length > maxBytes) {
      byte[] truncatedBytes = Arrays.copyOf(content.getBytes(StandardCharsets.UTF_8), maxBytes - ABBREVIATION.length());
      return new String(truncatedBytes, StandardCharsets.UTF_8) + ABBREVIATION;
    }
    return content;
  }
}
