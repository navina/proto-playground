package com.navina.protobuf;

import com.google.protobuf.util.JsonFormat;
import com.q6cyber.dao.kafka.KafkaBnrUrl;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.pinot.plugin.inputformat.protobuf.ProtoBufMessageDecoder;
import org.apache.pinot.spi.data.readers.GenericRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProtobufTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProtobufTest.class);
  private static final String DATA_FILE_NAME = "/Users/navina/Downloads/generated_kbu_proto.protobin";

  public void doTest() {
    ProtoBufMessageDecoder decoder = null;
    try {
      decoder = getProtoMessageDecoder();

      GenericRow reuse = new GenericRow();

      try (FileInputStream fis = new FileInputStream(DATA_FILE_NAME)) {
        KafkaBnrUrl url;
        while ( (url = KafkaBnrUrl.parseDelimitedFrom(fis)) != null) {
//          String jsonString = JsonFormat.printer().print(url);
//          System.out.println(jsonString);
          byte[] bytes = url.toByteArray();
          reuse.clear();
          decoder.decode(bytes, reuse);
          System.out.println(reuse);
        }
      } catch (IOException e) {
        LOGGER.error("Failed to open input file", e);
      }
    } catch (Exception e) {
      LOGGER.error("Failed to setup decoder correctly", e);
    }
  }

  public ProtoBufMessageDecoder getProtoMessageDecoder() {
    Map<String, String> decoderProps = new HashMap<>();
    URL descriptorFile = getClass().getClassLoader().getResource("descriptors_release_grpc-1.85.desc");
    try {
      decoderProps.put("descriptorFile", descriptorFile.toURI().toString());
      decoderProps.put("protoClassName", "kafka.KafkaBnrUrl");
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    ProtoBufMessageDecoder messageDecoder = new ProtoBufMessageDecoder();
    try {
      messageDecoder.init(decoderProps, null, "");
    } catch (Exception e) {
      e.printStackTrace();
    }
    return messageDecoder;
  }

  public static void main(String[] args) {
    ProtobufTest testObj = new ProtobufTest();
    testObj.doTest();
  }


}
