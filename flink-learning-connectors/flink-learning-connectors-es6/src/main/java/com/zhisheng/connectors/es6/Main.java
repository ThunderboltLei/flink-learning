package com.zhisheng.connectors.es6;


import com.zhisheng.common.model.Metrics;
import com.zhisheng.common.utils.ExecutionEnvUtil;
import com.zhisheng.common.utils.GsonUtil;
import com.zhisheng.common.utils.KafkaConfigUtil;
import com.zhisheng.connectors.es6.utils.ElasticSearchSinkUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.xcontent.XContentType;

import java.util.List;

import static com.zhisheng.common.constant.PropertiesConstants.*;

/**
 * blog：http://www.54tianzhisheng.cn/
 * 微信公众号：zhisheng
 */
@Slf4j
public class Main {
    public static void main(String[] args) throws Exception {
        final ParameterTool parameterTool = ExecutionEnvUtil.createParameterTool(args);
        StreamExecutionEnvironment env = ExecutionEnvUtil.prepare(parameterTool);
        DataStreamSource<Metrics> data = KafkaConfigUtil.buildSource(env);

        List<HttpHost> esAddresses = ElasticSearchSinkUtil.getEsAddresses(parameterTool.get(ELASTICSEARCH_HOSTS));
        int bulkSize = parameterTool.getInt(ELASTICSEARCH_BULK_FLUSH_MAX_ACTIONS, 40);
        int sinkParallelism = parameterTool.getInt(STREAM_SINK_PARALLELISM, 5);

        log.info("-----esAddresses = {}, parameterTool = {}, ", esAddresses, parameterTool);

        ElasticSearchSinkUtil.addSink(esAddresses, bulkSize, sinkParallelism, data,
                (Metrics metric, RuntimeContext runtimeContext, RequestIndexer requestIndexer) -> {
                    requestIndexer.add(Requests.indexRequest()
                            .index(ZHISHENG + "_" + metric.getName())
                            .type(ZHISHENG)
                            .source(GsonUtil.toJSONBytes(metric), XContentType.JSON));
                });
        env.execute("flink learning connectors es6");
    }
}
