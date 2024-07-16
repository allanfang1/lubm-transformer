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
        Model defaultModel = ModelFactory.createDefaultModel();
        System.out.println("Reading");
        FileUtils.iterateFiles(new File("./"), new String[]{"owl"}, false).forEachRemaining(file -> {
            final Model model = RDFDataMgr.loadModel(file.getAbsolutePath());

            defaultModel.add(model);
        });

        System.out.println("Attaching reasoning");

        Model schema = RDFDataMgr.loadModel("https://swat.cse.lehigh.edu/onto/univ-bench.owl");
        Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
        reasoner = reasoner.bindSchema(schema);

        InfModel infmodel = ModelFactory.createInfModel(reasoner, defaultModel);

        new File("./out").mkdirs();
        System.out.println("Writing JSONLD");
        RDFDataMgr.write(new FileOutputStream("./out/data_infer.jsonld"), infmodel, RDFFormat.JSONLD11_FLAT);

        System.out.println("Converting JSONLD");
        try {
            Map<String, Boolean> config = new HashMap<>();
            config.put(JsonGenerator.PRETTY_PRINTING, true);
            JsonWriterFactory writerFactory = Json.createWriterFactory(config);

            JsonArray expanded = JsonLd.expand(JsonDocument.of(new FileInputStream("./out/data_infer.jsonld"))).get();
            writerFactory.createWriter(new FileOutputStream("./out/data_infer_exp.jsonld")).write(expanded);

            JsonStructure flattened = JsonLd.flatten(JsonDocument.of(new FileInputStream("./out/data_infer.jsonld"))).get();
            writerFactory.createWriter(new FileOutputStream("./out/data_infer_fla.jsonld")).write(flattened);
        } catch (JsonLdError e) {
            throw new RuntimeException(e);
        }

        System.out.println("Writing NT");
        RDFDataMgr.write(new FileOutputStream("./out/data_infer.nt"), infmodel, Lang.NT);
    }
}