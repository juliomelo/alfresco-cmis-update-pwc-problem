package tests;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;

public abstract class UpdatePropertiesTest {
    private static final Random random = new Random();

    public static void main(final String args[]) {
        final String user = args.length == 0 ? System.console().readLine("User: ") : args[0];
        final String password = args.length < 2 ? new String(System.console().readPassword("Password: ")) : args[1];
        final Session session = createSession(user, password);

        final Folder home = (Folder) session.getObjectByPath("/Guest Home");
        
        System.out.println("================================");
        System.out.println("First test: without working copy");
        System.out.println("--------------------------------");
        final Document firstDoc = createDocument(home);
        addOwner(firstDoc, "The owner"); // This works!

        System.out.println("==============================");
        System.out.println("Second test: with working copy");
        System.out.println("------------------------------");
        final Document secondDoc = createDocument(home);
        final Document pwc = (Document) session.getObject(secondDoc.checkOut());
        addOwner(pwc, "The owner"); // This fails on PWC!

        /* org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException: Constraint violation: 06120054 Found 1 integrity violations:
         * The association target multiplicity has been violated: 
         *    Source Node: workspace://SpacesStore/448b9a50-def0-493a-a4eb-a1e7b3760d09
         *    Association: Association[ class=ClassDef[name={http://www.alfresco.org/model/content/1.0}checkedOut], name={http://www.alfresco.org/model/content/1.0}workingcopylink, target class={http://www.alfresco.org/model/content/1.0}workingcopy, source role=null, target role=null]
         *    Required target Multiplicity: 1..1
         *    Actual target Multiplicity: 0
         */

        final ObjectId id = pwc.checkIn(true, null, null, "Test completed!");
        System.out.println("Test succeeded. Final id: " + id.getId());
    }

    private static Document createDocument(final Folder folder) {
        final String fileName = String.format("test-%d-%d.txt", new Date().getTime(), random.nextInt());

        final HashMap<String, Object> props = new HashMap<>();
        props.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
        props.put(PropertyIds.NAME, fileName);

        final ContentStream cs = new ContentStreamImpl(fileName, "text/plain",
                "This is just a test - " + new Date().toString());

        final Document document = folder.createDocument(props, cs, VersioningState.MAJOR);

        System.out.println(
            String.format("Created document: %s; Type: %s; Secondary-Type: %s",
                document.getId(),
                document.getPropertyValue(PropertyIds.OBJECT_TYPE_ID),
                document.getPropertyValue(PropertyIds.SECONDARY_OBJECT_TYPE_IDS)));

        return document;
    }

    private static Document addOwner(Document doc, final String owner) {
        final HashMap<String, Object> props = new HashMap<>();
        props.put("cm:owner", owner);

        System.out.println(
            String.format("Adding owner %s to document %s; Type: %s; Secondary-Type: %s",
                owner,
                doc.getId(),
                doc.getPropertyValue(PropertyIds.OBJECT_TYPE_ID),
                doc.getPropertyValue(PropertyIds.SECONDARY_OBJECT_TYPE_IDS)));

        doc = (Document) doc.updateProperties(props, Arrays.asList("P:cm:ownable"), null);

        System.out.println(
            String.format("Updated dDocument: %s; Type: %s; Secondary-Type: %s; Owner: %s",
                doc.getId(),
                doc.getPropertyValue(PropertyIds.OBJECT_TYPE_ID),
                doc.getPropertyValue(PropertyIds.SECONDARY_OBJECT_TYPE_IDS),
                doc.getPropertyValue("cm:owner")));
        
        return doc;
    }
    private static Session createSession(final String user, final String password) {
        try {
            System.out.println("Creating CMIS session...");

            final Map<String, String> params = createSessionParameters(user, password);
            final SessionFactory factory = SessionFactoryImpl.newInstance();
            final Repository repo = factory.getRepositories(params).get(0);

            params.put(SessionParameter.REPOSITORY_ID, repo.getId());

            System.out.println("Connecting...");
            Session session = factory.createSession(params);

            return session;
        } catch (CmisConnectionException e) {
            System.err.println(e.getMessage() + " Retrying...");
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
            }

            return createSession(user, password);
        }
    }

    private static Map<String, String> createSessionParameters(final String user, final String password) {
        final Map<String, String> params = new HashMap<>();

        params.put(SessionParameter.USER, user);
        params.put(SessionParameter.PASSWORD, password);

        params.put(SessionParameter.ATOMPUB_URL, "http://alfresco:8080/alfresco/api/-default-/public/cmis/versions/1.1/atom");
        params.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());

        return params;
    }

}
