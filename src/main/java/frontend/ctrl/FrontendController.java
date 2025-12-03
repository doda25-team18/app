package frontend.ctrl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Objects;

import doda25.team18.VersionUtil;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import frontend.data.Sms;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(path = "/sms")
public class FrontendController {

    private String modelHost;

    private int numPredictions = 0;
    private int correctPredictions = 0;
    private ArrayList<Float> predictionDelays = new ArrayList<>();
    static final float[] predictionBuckets = {0.02f, 0.05f, 0.1f, 0.2f, 0.5f};

    private RestTemplateBuilder rest;

    public FrontendController(RestTemplateBuilder rest, Environment env) {
        this.rest = rest;
        this.modelHost = env.getProperty("MODEL_HOST");
        assertModelHost();
    }

    private void assertModelHost() {
        if (modelHost == null || modelHost.strip().isEmpty()) {
            System.err.println("ERROR: ENV variable MODEL_HOST is null or empty");
            System.exit(1);
        }
        modelHost = modelHost.strip();
        if (modelHost.indexOf("://") == -1) {
            var m = "ERROR: ENV variable MODEL_HOST is missing protocol, like \"http://...\" (was: \"%s\")\n";
            System.err.printf(m, modelHost);
            System.exit(1);
        } else {
            System.out.printf("Working with MODEL_HOST=\"%s\"\n", modelHost);
        }
    }

    @GetMapping("")
    public String redirectToSlash(HttpServletRequest request) {
        // relative REST requests in JS will end up on / and not on /sms
        return "redirect:" + request.getRequestURI() + "/";
    }

    @GetMapping("/")
    public String index(Model m) {
        m.addAttribute("hostname", modelHost);
        m.addAttribute("libVersion", VersionUtil.getVersion());
        return "sms/index";
    }

    @PostMapping({ "", "/" })
    @ResponseBody
    public Sms predict(@RequestBody Sms sms) {
        System.out.printf("Requesting prediction for \"%s\" ...\n", sms.sms);
        sms.result = getPrediction(sms);
        System.out.printf("Prediction: %s\n", sms.result);
        if(Objects.equals(sms.guess, sms.result)) correctPredictions++;
        numPredictions++;
        return sms;
    }

    private String getPrediction(Sms sms) {
        try {
            var url = new URI(modelHost + "/predict");
            long start = System.nanoTime();
            var c = rest.build().postForEntity(url, sms, Sms.class);
            long end = System.nanoTime();
            float secondsElapsed = (end - start) / 1_000_000_000f;
            predictionDelays.add(secondsElapsed);
            return c.getBody().result.trim();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    @GetMapping({"/metrics", "/metrics/"})
    @ResponseBody
    public String getMetrics() {
        StringBuilder m = new StringBuilder("# HELP num_predictions This is the total amount of predictions that were requested and handled\n");
        m.append("# TYPE num_predictions counter\n");
        m.append("num_predictions ").append(numPredictions).append("\n\n");

        m.append("# HELP correct_predictions This is the fraction of predictions that were correct\n");
        m.append("# TYPE correct_predictions gauge\n");
        m.append("correct_predictions ").append((float) correctPredictions / numPredictions).append("\n\n");

        m.append("# HELP predict_latency This is how long it took to get a response from the model service in seconds\n");
        m.append("# TYPE predict_latency histogram\n");
        for(float bucket : predictionBuckets) {
            m.append("predict_latency_bucket{le=\"").append(bucket).append("\"} ").append(predictionDelays.stream().filter(x -> x <= bucket).count()).append("\n");
        }
        m.append("predict_latency_bucket{le=\"+Inf\"} ").append(predictionDelays.size()).append("\n");
        m.append("predict_latency_sum ").append(predictionDelays.stream().mapToDouble(Float::doubleValue).sum()).append("\n");
        m.append("predict_latency_count" ).append(predictionDelays.size()).append("\n");
        return m.toString();
    }
}