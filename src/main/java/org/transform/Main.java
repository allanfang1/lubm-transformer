package org.transform;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        Model defaultModel = ModelFactory.createDefaultModel();
        System.out.println("reading");
        FileUtils.iterateFiles(new File("./"), new String[]{"owl"}, false).forEachRemaining(file -> {
            final Model model = RDFDataMgr.loadModel(file.getAbsolutePath());

            defaultModel.add(model);
        });

        System.out.println("reasoning");

        Model schema = RDFDataMgr.loadModel("https://swat.cse.lehigh.edu/onto/univ-bench.owl");
        Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
        reasoner = reasoner.bindSchema(schema);

        InfModel infmodel = ModelFactory.createInfModel(reasoner, defaultModel);
        new File("./out").mkdirs();
        RDFWriter.create(infmodel).format(RDFFormat.JSONLD_EXPAND_PRETTY).output("./out/data_infer_exp.jsonld");
        RDFWriter.create(infmodel).format(RDFFormat.JSONLD_FLATTEN_PRETTY).output("./out/data_infer_flat.jsonld");
        RDFWriter.create(infmodel).format(RDFFormat.NT).output("./out/data_infer.nt");
    }
}