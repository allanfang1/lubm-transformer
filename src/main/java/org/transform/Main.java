package org.transform;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        String inPath = args[0];
        String outPath = args[1];
        Model schema = RDFDataMgr.loadModel("https://swat.cse.lehigh.edu/onto/univ-bench.owl");
        Reasoner reasoner = ReasonerRegistry.getOWLReasoner().bindSchema(schema);

        System.out.println("Reasoning");
        File temp = new File("./temp");
        temp.mkdirs();
        FileUtils.iterateFiles(new File(inPath), new String[]{"owl"}, false).forEachRemaining(file -> {
            final Model model = RDFDataMgr.loadModel(file.getAbsolutePath());
            InfModel infmodel = ModelFactory.createInfModel(reasoner, model);
            try {
                System.out.println(file.getName());
                RDFDataMgr.write(new FileOutputStream("./temp/" + file.getName() + ".jsonld"), infmodel, RDFFormat.JSONLD11_FLAT);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        System.out.println("Merging JSONLD");
        new File(outPath).mkdirs();
        Model defaultModel = ModelFactory.createDefaultModel();
        FileUtils.iterateFiles(new File("./temp/"), new String[]{"jsonld"}, false).forEachRemaining(file -> {
            final Model model = RDFDataMgr.loadModel(file.getAbsolutePath());
            defaultModel.add(model);
        });
        RDFDataMgr.write(new FileOutputStream(outPath + "/data_infer.jsonld"), defaultModel, RDFFormat.JSONLD11_FLAT);

        System.out.println("Formatting JSONLD");
        try {
            Map<String, Boolean> config = new HashMap<>();
            config.put(JsonGenerator.PRETTY_PRINTING, true);
            JsonWriterFactory writerFactory = Json.createWriterFactory(config);

            JsonArray expanded = JsonLd.expand(JsonDocument.of(new FileInputStream(outPath + "/data_infer.jsonld"))).get();
            writerFactory.createWriter(new FileOutputStream(outPath + "/data_infer_exp.jsonld")).write(expanded);

            JsonStructure flattened = JsonLd.flatten(JsonDocument.of(new FileInputStream(outPath + "/data_infer.jsonld"))).get();
            writerFactory.createWriter(new FileOutputStream(outPath + "/data_infer_fla.jsonld")).write(flattened);

            FileUtils.deleteDirectory(temp);
        } catch (JsonLdError e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Writing NT");
        RDFDataMgr.write(new FileOutputStream(outPath + "/data_infer.nt"), defaultModel, Lang.NT);

        System.out.println("Finished");
    }
}