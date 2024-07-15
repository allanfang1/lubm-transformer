package org.transform;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
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

        System.out.println("writing");
        new File("./out").mkdirs();
        RDFDataMgr.write(new FileOutputStream("./out/data_infer_exp.jsonld"), infmodel, Lang.JSONLD); //TODO: flatten
        RDFDataMgr.write(new FileOutputStream("./out/data_infer.nt"), infmodel, Lang.NT);
    }
}