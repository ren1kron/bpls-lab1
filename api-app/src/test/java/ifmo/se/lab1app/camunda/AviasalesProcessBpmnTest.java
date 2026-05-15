package ifmo.se.lab1app.camunda;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class AviasalesProcessBpmnTest {

    private static final String BPMN_NS = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    private static final String BPMNDI_NS = "http://www.omg.org/spec/BPMN/20100524/DI";
    private static final String CAMUNDA_NS = "http://camunda.org/schema/1.0/bpmn";

    @Test
    void shouldDeclareExecutableCamundaProcessFormsMessagesAndExternalTopics() throws Exception {
        Document document = parseProcess();
        Element process = (Element) document.getElementsByTagNameNS(BPMN_NS, "process").item(0);

        assertThat(process.getAttribute("id")).isEqualTo("aviasales-campaign-launch");
        assertThat(process.getAttribute("isExecutable")).isEqualTo("true");
        assertThat(document.getElementsByTagNameNS(CAMUNDA_NS, "formData").getLength())
                .isGreaterThanOrEqualTo(8);

        Set<String> topics = elements(document, "serviceTask").stream()
                .map(element -> element.getAttributeNS("http://camunda.org/schema/1.0/bpmn", "topic"))
                .filter(topic -> !topic.isBlank())
                .collect(Collectors.toSet());
        assertThat(topics).contains(
                "campaign-create-draft",
                "campaign-configure",
                "creative-schedule-upload",
                "moderation-decision",
                "payment-succeeded",
                "payment-canceled",
                "payment-retry-invoice",
                "campaign-activate",
                "campaign-freeze",
                "campaign-resume",
                "campaign-stop"
        );

        Set<String> messages = elements(document, "message").stream()
                .map(element -> element.getAttribute("name"))
                .collect(Collectors.toSet());
        assertThat(messages).contains(
                "CreativeUploadFinished",
                "PaymentSucceeded",
                "PaymentCanceled",
                "PauseRequested"
        );

        assertThat(document.getElementsByTagNameNS(BPMNDI_NS, "BPMNDiagram").getLength()).isEqualTo(1);
        Element plane = (Element) document.getElementsByTagNameNS(BPMNDI_NS, "BPMNPlane").item(0);
        assertThat(plane.getAttribute("bpmnElement")).isEqualTo("aviasales-campaign-launch");
        assertThat(document.getElementsByTagNameNS(BPMNDI_NS, "BPMNShape").getLength()).isGreaterThan(0);
        assertThat(document.getElementsByTagNameNS(BPMNDI_NS, "BPMNEdge").getLength()).isGreaterThan(0);
    }

    private Document parseProcess() throws Exception {
        Path rootRelative = Path.of("docs", "aviasales-process.bpmn");
        Path moduleRelative = Path.of("..", "docs", "aviasales-process.bpmn");
        return parse(Files.exists(rootRelative) ? rootRelative : moduleRelative);
    }

    private Document parse(Path path) throws Exception {
        assertThat(Files.exists(path)).isTrue();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(path.toFile());
    }

    private java.util.List<Element> elements(Document document, String localName) {
        var nodes = document.getElementsByTagNameNS(BPMN_NS, localName);
        java.util.ArrayList<Element> elements = new java.util.ArrayList<>();
        for (int index = 0; index < nodes.getLength(); index++) {
            elements.add((Element) nodes.item(index));
        }
        return elements;
    }
}
