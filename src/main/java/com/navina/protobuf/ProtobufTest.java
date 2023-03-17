package com.navina.protobuf;

import java.io.FileInputStream;
import java.io.IOException;


public class ProtobufTest {
  public static void main(String[] args) {
    try (FileInputStream fis = new FileInputStream("/Users/navina/Downloads/generated_kbu_proto.protobin")) {
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
