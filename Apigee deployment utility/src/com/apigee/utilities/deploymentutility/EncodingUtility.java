package com.apigee.utilities.deploymentutility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

/**
 * User: Darshan
 * Date: 03/22/2015
 * Time: 11:48 AM
 */
public class EncodingUtility {

    public static void main(String[] args) throws IOException {

//        InputStreamReader inp = new InputStreamReader(System.in);
//        BufferedReader br = new BufferedReader(inp);
//
//        System.out.println("Enter text : ");
//
//        String texttoencode = br.readLine();
//
//        // Create the encoder and decoder for ISO-8859-1
//        //Charset charset = Charset.forName("ISO-8859-1");
//        Charset charset = Charset.forName("Shift-JIS");
//        CharsetDecoder decoder = charset.newDecoder();
//        CharsetEncoder encoder = charset.newEncoder();
//
//
//        try {
//            // Convert a string to ISO-LATIN-1 bytes in a ByteBuffer
//            // The new ByteBuffer is ready to be read.
//            ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(texttoencode));
//
//            // Convert ISO-LATIN-1 bytes in a ByteBuffer to a character ByteBuffer and then to a string.
//            // The new ByteBuffer is ready to be read.
//            CharBuffer cbuf = decoder.decode(bbuf);
//            String s = cbuf.toString();
//            System.out.println(s);
//        } catch (CharacterCodingException e) {
//            e.printStackTrace();
//
//        }

        String toEncode = "http://175.41.200.78/apigeepoc/openid";

        String encoded = URLEncoder.encode(toEncode, "ASCII");
        encoded = encoded.replace(".", "%2e");

        System.out.println(encoded);
        System.out.println(System.currentTimeMillis());



    }
}
