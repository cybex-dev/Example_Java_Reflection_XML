import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.util.Random;

public class Main {

    List<Clazz> clazzLists = new ArrayList<>();

    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        try {
            readXml();
            createStudents();
            writeXml();
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | XPathExpressionException e) {
            e.printStackTrace();
        }
    }

    private void createStudents() {
        Random random = new Random();
        int numStudentsToAdd = random.nextInt(5);
        System.out.println("Adding " + numStudentsToAdd + " Students");
        for (int i = 0; i < numStudentsToAdd; i++) {
            int Class = random.nextInt(3);
            int id = random.nextInt();
            int age = random.nextInt(80);
            boolean isMale = random.nextBoolean();
            System.out.printf("[%d]\tAdded Student to Class #%d: Student [ id = %d ; age = %d ; Male? = %b ]\n", i, Class + 1, id, age, isMale);
            clazzLists.get(Class).addStudent(new Student(id, age, isMale));
        }
    }

    private void readXml() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, XPathExpressionException {

        System.out.println("Reading document");
        // Build Document object from XML file
        Document document = getDocument();

        // Get all "class" tag components
        // alternatively, you can use XPath for queries (see method 2 in link) : https://ohnoimwritingwrap.wordpress.com/2017/03/16/read-from-xml/
        // Have a look at the various ways an XPath query can be constructed - kinda like SQL in Databases, but just for XML files
        NodeList nodesClassList = document.getElementsByTagName("Clazz");
        // NOTE: alternatively, you can get the firstChild element of the document element with a recursive call. This will get the document element i.e. <classes> and call getFirstChild on the document element, this will get the first instance of <class> in the <classes> element.
        for (int i = 0; i < nodesClassList.getLength(); i++) {
            Node item = nodesClassList.item(i);
            // Check if we have a text node
            if (item.getNodeType() != Element.TEXT_NODE) {
                // get Clazz class
                Class<?> aClass = Class.forName(item.getNodeName());
                // create instance of Clazz class
                Clazz clazz = (Clazz) aClass.getConstructor(String.class, String.class).newInstance(item.getAttributes().getNamedItem("id").getNodeValue(), item.getAttributes().getNamedItem("room").getNodeValue());

                // look through current item and find any students to add into clazz object
                // lets do this using XPath
                String xpath = "//Clazz[" + (i + 1) + "]/Student";
                NodeList childNodeList = (NodeList) XPathFactory.newInstance().newXPath().compile(xpath).evaluate(document, XPathConstants.NODESET);
                if (childNodeList.getLength() != 0) {
                    // get add student method because there are students to add
                    Method method = null;
                    for (Method aClassMethod : aClass.getMethods()) {
                        if (aClassMethod.getName().equals("addStudent")) {
                            // Found method
                            method = aClassMethod;
                            break;
                        }
                    }

                    if (method == null) {
                        System.out.println("Cannot find addStudent method");
                        return;
                    }

                    // Add students
                    for (int j = 0; j < childNodeList.getLength(); j++) {
                        Node child = childNodeList.item(j);
                        Student student = createStudentFromXMLItem(child);
                        //add student to clazz
                        method.invoke(clazz, student);
                    }
                }

                clazzLists.add(clazz);
            }
        }
    }

    private Student createStudentFromXMLItem(Node item) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException {
        // get each child item, get class, create object instance, use reflection to get methods and add data
        // I changed Student constructor, so name can only be set via methods. Constructor does not take name any more.
        Class<?> childClass = Class.forName(item.getNodeName());
        Constructor<?> constructor = childClass.getConstructor(int.class, int.class, boolean.class);
        int id = Integer.parseInt(item.getAttributes().getNamedItem("id").getNodeValue());
        int age = Integer.parseInt(item.getAttributes().getNamedItem("age").getNodeValue());
        boolean isMale = Boolean.parseBoolean(item.getAttributes().getNamedItem("isMale").getNodeValue());
        Object o = constructor.newInstance(id, age, isMale);

        // Get the setName method. Note that this is a private method
        Method setNameMethod = childClass.getDeclaredMethod("setName", String.class);

        // enable using private methods
        setNameMethod.setAccessible(true);

        // Set name using method invoke
        setNameMethod.invoke(o, item.getAttributes().getNamedItem("name").getNodeValue());

        // return child student
        return (Student) o;
    }

    private void writeXml() {
        // Get Document
        Document doc = getDocument();

        // clear existing document
        doc.removeChild(doc.getDocumentElement());

        // Build Document Tree
        // create root element <Clazzes>
        Element clazzes = doc.createElement("Clazzes");
        doc.appendChild(clazzes);

        // add classes to clazzes
        clazzLists.forEach(clazz -> {
            try {
                Element clazzElement = createElement(doc, clazz);
                clazzes.appendChild(clazzElement);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });

        // Write Document to file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //Indent elements
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource domSource = new DOMSource(doc);
            StreamResult streamResult = new StreamResult(new File("data.xml"));
            transformer.transform(domSource, streamResult);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    private Element createElement(Document doc, Object object) throws IllegalAccessException {
        Element element = doc.createElement(object.getClass().getName());
        for (Field field : object.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object o = field.get(object);
            if (o instanceof String) {
                element.setAttribute(field.getName(), String.valueOf(o));
            } else if (o instanceof Integer) {
                element.setAttribute(field.getName(), String.valueOf(o));
            } else if (o instanceof Boolean) {
                element.setAttribute(field.getName(), String.valueOf(o));
            } else if (o instanceof ArrayList) {
                for (Object arrayElement : ((ArrayList) o)) {
                    Element childElement = createElement(doc, arrayElement);
                    element.appendChild(childElement);
                }
            }
        }
        return element;
    }

    Document getDocument() {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            InputStream IS = new FileInputStream("data.xml");
            Document doc = documentBuilder.parse(IS);
            return doc;
        } catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
            return null;
        }
    }


}
