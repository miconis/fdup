package eu.dnetlib;

import eu.dnetlib.graph.GraphProcessor;
import eu.dnetlib.pace.config.DedupConfig;
import eu.dnetlib.pace.model.MapDocument;
import eu.dnetlib.pace.util.BlockProcessor;
import eu.dnetlib.pace.util.MapDocumentUtil;
import eu.dnetlib.pace.utils.Utility;
import eu.dnetlib.reporter.SparkReporter;
import eu.dnetlib.support.ConnectedComponent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.graphx.Edge;
import org.apache.spark.rdd.RDD;
import org.apache.spark.util.LongAccumulator;
import scala.Serializable;
import scala.Tuple2;

import java.util.Map;
import java.util.stream.Collectors;

public class Deduper implements Serializable {

    private static final Log log = LogFactory.getLog(Deduper.class);

    /**
     * @param: the spark context
     * @param: list of JSON entities to be deduped
     * @param: the dedup configuration
     *
     * @return the list of connected components generated by the deduplication
     */
    public static JavaRDD<ConnectedComponent> dedup(JavaSparkContext context, JavaRDD<String> entities, DedupConfig config){

        Map<String, LongAccumulator> accumulators = Utility.constructAccumulator(config, context.sc());

        //create vertexes of the graph: <ID, MapDocument>
        JavaPairRDD<String, MapDocument> mapDocs = mapToVertexes(context, entities, config);
        RDD<Tuple2<Object, MapDocument>> vertexes = mapDocs.mapToPair(t -> new Tuple2<Object, MapDocument>( (long) t._1().hashCode(), t._2())).rdd();

        //create blocks for deduplication
        JavaPairRDD<String, Iterable<MapDocument>> blocks = createBlocks(context, mapDocs, config);

        //create relations by comparing only elements in the same group
        final JavaPairRDD<String, String> relationRDD = computeRelations(context, blocks, config);

        final RDD<Edge<String>> edgeRdd = relationRDD.map(it -> new Edge<>(it._1().hashCode(),it._2().hashCode(), "equalTo")).rdd();

        accumulators.forEach((name, acc) -> log.info(name + " -> " + acc.value()));

        return GraphProcessor.findCCs(vertexes, edgeRdd, config.getWf().getMaxIterations()).toJavaRDD();
    }

    /**
     * @param: the spark context
     * @param: list of blocks
     * @param: the dedup configuration
     *
     * @return the list of relations generated by the deduplication
     */
    public static JavaPairRDD<String, String> computeRelations(JavaSparkContext context, JavaPairRDD<String, Iterable<MapDocument>> blocks, DedupConfig config) {

        Map<String, LongAccumulator> accumulators = Utility.constructAccumulator(config, context.sc());
        return blocks.flatMapToPair(it -> {
            final SparkReporter reporter = new SparkReporter(accumulators);
            new BlockProcessor(config).process(it._1(), it._2(), reporter);
            return reporter.getRelations().iterator();
        });
    }

    /**
     * @param: the spark context
     * @param: list of entities: <id, entity>
     * @param: the dedup configuration
     *
     * @return the list of blocks based on clustering of dedup configuration
     */
    public static JavaPairRDD<String, Iterable<MapDocument>> createBlocks(JavaSparkContext context, JavaPairRDD<String, MapDocument> mapDocs, DedupConfig config) {

        return mapDocs.reduceByKey((a, b) -> a)    //the reduce is just to be sure that we haven't document with same id
                //Clustering: from <id, doc> to List<groupkey,doc>
                .flatMapToPair(a -> {
                    final MapDocument currentDocument = a._2();

                    return Utility.getGroupingKeys(config, currentDocument).stream()
                            .map(it -> new Tuple2<>(it, currentDocument)).collect(Collectors.toList()).iterator();
                }).groupByKey();
    }

    /**
     * @param: the spark context
     * @param: list of JSON entities
     * @param: the dedup configuration
     *
     * @return the list of vertexes: <id, mapDocument>
     */
    public static JavaPairRDD<String, MapDocument> mapToVertexes(JavaSparkContext context, JavaRDD<String> entities, DedupConfig config){
        return entities.mapToPair(it -> {
            MapDocument mapDocument = MapDocumentUtil.asMapDocumentWithJPath(config, it);
            return new Tuple2<>(mapDocument.getIdentifier(), mapDocument);
        });
    }

}