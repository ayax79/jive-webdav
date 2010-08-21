package com.syncapse.jive.webdav.fromcatalina

/**
 * Created by IntelliJ IDEA.
 * User: andrew
 * Date: Aug 21, 2010
 * Time: 10:07:33 AM
 * To change this template use File | Settings | File Templates.
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.BitSet;

/**
 *
 * This class is very similar to the java.net.URLEncoder class.
 *
 * Unfortunately, with java.net.URLEncoder there is no way to specify to the
 * java.net.URLEncoder which characters should NOT be encoded.
 *
 * This code was moved from DefaultServlet.java
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 */

object URLEncoder {

  val HEXIDECIMAL = '0' :: '1' :: '2' :: '3' :: '4' :: '5' :: '6' :: '7' :: '8' :: '9' :: 'A' :: 'B' :: 'C' ::'D' :: 'E' :: 'F' :: None


}

class URLEncoder {

    // Array containing the safe characters set.
    val _safeCharacters = new BitSet(256);

    for (i <- 'a' until('z')) addSafeCharacter(i);
    for (i <- 'A' until('Z')) addSafeCharacter(i);
    for (i <- '0' until('9')) addSafeCharacter(i);


    def addSafeCharacter(c: Char) = {
        _safeCharacters.set(c);
    }

    def encode(path: String): String =  {
        val maxBytesPerChar = 10;
        // int caseDiff = ('a' - 'A');
        val rewrittenPath = new StringBuffer(path.length());
        val buf = new ByteArrayOutputStream(maxBytesPerChar);
        val writer = null;
        try {
            writer = new OutputStreamWriter(buf, "UTF8");
        } catch {
          case e: Exception =>
            e.printStackTrace();
            writer = new OutputStreamWriter(buf);
        }

        for (i <- 0 until(path.length)) {
            int c = path.charAt(i).toInt;
            if (_safeCharacters.get(c)) {
                rewrittenPath.append((char) c);
            } else {
                // convert to external encoding before hex conversion
                try {
                    writer.write((char) c);
                    writer.flush();
                } catch {
                    case i: IOException =>
                      buf.reset();
                      continue;
                }
                val ba = buf.toByteArray();
                for (j <- 0 until(ba.length)) {
                    // Converting each byte in the buffer
                    val toEncode = ba.apply(j)
                    rewrittenPath.append('%')
                    val low = (toEncode & 0x0f).toInt
                    val high = ((toEncode & 0xf0) >> 4).toInt
                    rewrittenPath.append(HEXADECIMAL.apply(high))
                    rewrittenPath.append(HEXADECIMAL.apply(low))
                }
                buf.reset();
            }
        }
        rewrittenPath.toString();
    }
}
