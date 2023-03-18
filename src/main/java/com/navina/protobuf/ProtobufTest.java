package com.navina.protobuf;

import com.q6cyber.dao.kafka.KafkaBnrUrl;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.pinot.plugin.inputformat.protobuf.ProtoBufMessageDecoder;
import org.apache.pinot.segment.local.segment.creator.TransformPipeline;
import org.apache.pinot.segment.local.utils.IngestionUtils;
import org.apache.pinot.spi.config.table.TableConfig;
import org.apache.pinot.spi.data.Schema;
import org.apache.pinot.spi.data.readers.GenericRow;
import org.apache.pinot.spi.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProtobufTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProtobufTest.class);
  // Replace the following to correct filenames
  private static final String DATA_FILE_NAME = "/Users/navina/Downloads/generated_kbu_proto.protobin";
  private static final String DESCRIPTOR_FILE = "descriptors_release_grpc-1.85.desc";


  // TODO: Not sure why these files don't show up when used as a main resource. Adding full compiled path. Fix it!
  private static final String TABLE_CONFIG_FILE = "/Users/navina/Projects/proto-playground/target/classes/table_config.json";
  private static final String SCHEMA_FILE = "/Users/navina/Projects/proto-playground/target/classes/table_schema.json";

  public void doTest() {
    ProtoBufMessageDecoder decoder = null;
    try {
      decoder = getProtoMessageDecoder(false);
      TransformPipeline transformPipeline = new TransformPipeline(getTableConfig(), getTableSchema());

      GenericRow reuse = new GenericRow();
      TransformPipeline.Result result = new TransformPipeline.Result();

      try (FileInputStream fis = new FileInputStream(DATA_FILE_NAME)) {
        KafkaBnrUrl url;
        while ( (url = KafkaBnrUrl.parseDelimitedFrom(fis)) != null) {
          if (StringUtils.isBlank(url.getUrl().getHost())){
            throw new NullPointerException("no empty hosts");
          }
          byte[] bytes = url.toByteArray();
          reuse.clear();
          decoder.decode(bytes, reuse);
          System.out.println("Desrialized and Extracted Record");
/*          if (reuse.hasNullValues()) {
            System.out.println("Null values: ");
            reuse.getNullValueFields().forEach(System.out::println);
          }*/
          System.out.println(reuse);

          result.reset();
          transformPipeline.processRow(reuse, result);
/*          System.out.println("Transformed row(s)");
          result.getTransformedRows().forEach(row -> {
            if (row.hasNullValues()) {
              System.out.println("Null values: ");
              row.getNullValueFields().forEach(System.out::println);
            }
//            System.out.println(row.toString());
          });*/
          System.out.println("==========================");
        }
      } catch (IOException e) {
        LOGGER.error("Failed to open input file", e);
      }
    } catch (Exception e) {
      LOGGER.error("Failed to setup decoder correctly", e);
    }
  }

  public void produceToKafka() {
    Properties properties = new Properties();
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

    try (
        FileInputStream fis = new FileInputStream(DATA_FILE_NAME);
        KafkaProducer<byte[], byte[]> producer = new KafkaProducer<>(properties)) {
      KafkaBnrUrl url;
      while ((url = KafkaBnrUrl.parseDelimitedFrom(fis)) != null) {
        byte[] bytes = url.toByteArray();
        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>("url2", bytes);
        producer.send(record).get(30, TimeUnit.SECONDS);

      }
    } catch (IOException e) {
      LOGGER.error("Failed to open input file", e);
    } catch (ExecutionException | InterruptedException | TimeoutException e) {
      LOGGER.error("Failed to produce", e);
    }
  }

  public ProtoBufMessageDecoder getProtoMessageDecoder(boolean extractAllFields) {
    Map<String, String> decoderProps = new HashMap<>();
    URL descriptorFile = getClass().getClassLoader().getResource(DESCRIPTOR_FILE);
    try {
      decoderProps.put("descriptorFile", descriptorFile.toURI().toString());
      decoderProps.put("protoClassName", "kafka.KafkaBnrUrl");
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    ProtoBufMessageDecoder messageDecoder = new ProtoBufMessageDecoder();
    try {
      if (extractAllFields) {
        messageDecoder.init(decoderProps, null, "");
      } else {
        TableConfig tableConfig = getTableConfig();
        Schema tableSchema = getTableSchema();
        assert tableConfig != null;
        assert tableSchema != null;
        Set<String> fields = IngestionUtils.getFieldsForRecordExtractor(tableConfig.getIngestionConfig(), tableSchema);
        messageDecoder.init(decoderProps, fields, "");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return messageDecoder;
  }

  public TableConfig getTableConfig() {

    try (FileInputStream fis = new FileInputStream(TABLE_CONFIG_FILE)) {
      return JsonUtils.inputStreamToObject(fis, TableConfig.class);
    } catch (IOException e) {
      LOGGER.error("Failed to load table config", e);
    }

    return null;
  }

  public Schema getTableSchema() {
    try (FileInputStream fis = new FileInputStream(SCHEMA_FILE)) {
      return JsonUtils.inputStreamToObject(fis, Schema.class);
    } catch (IOException e) {
      LOGGER.error("Failed to load table config", e);
    }

    return null;
  }

  public static void main(String[] args) {
    ProtobufTest testObj = new ProtobufTest();
//    testObj.doTest();
    testObj.produceToKafka();
  }


}
