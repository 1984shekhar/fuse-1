package io.fabric8.openshift.commands;

import io.fabric8.utils.Strings;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.boot.commands.support.ContainerCreateSupport;
import io.fabric8.openshift.CreateOpenshiftContainerOptions;
import io.fabric8.utils.shell.ShellUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static io.fabric8.utils.FabricValidations.validateProfileName;

@Command(name = "container-create-openshift", scope = "fabric", description = "Creates one or more new containers on Openshift")
public class ContainerCreateOpenshift extends ContainerCreateSupport {

    @Option(name = "--server-url", required = false, description = "The url to the openshift server.")
    String serverUrl;

    @Option(name = "--login", required = false, description = "The login name to use.")
    String login;

    @Option(name = "--password", required = false, description = "The password to use.")
    String password;

    @Option(name = "--proxy-uri", description = "The Maven proxy URL to use")
    private URI proxyUri;

    @Argument(index = 0, required = true, description = "The name of the container to be created. When creating multiple containers it serves as a prefix")
    protected String name;

    @Option(name = "--gear-profile", description = "Gear profile controls how much memory and CPU your cartridges can use.")
    private String gearProfile;

    @Argument(index = 1, required = false, description = "The number of containers that should be created")
    protected int number = 0;

    private static final String OPENSHIFT_USER = "OPENSHIFT_USER";
    private static final String OPENSHIFT_USER_PASSWORD = "OPENSHIFT_USER_PASSWORD";

    @Override
    protected Object doExecute() throws Exception {
        // validate input before creating containers
        preCreateContainer(name);
        validateProfileName(profiles);

        if (session != null) {
            if (Strings.isNullOrBlank(login)) {
                login = (String) session.get(OPENSHIFT_USER);
            }

            if (Strings.isNullOrBlank(password)) {
                password = (String) session.get(OPENSHIFT_USER_PASSWORD);
            }
        }

        CreateOpenshiftContainerOptions.Builder builder = CreateOpenshiftContainerOptions.builder()
                .name(name)
                .serverUrl(serverUrl)
                .login(login)
                .password(password)
                .version(version)
                .number(number)
                .resolver("publichostname")
                .ensembleServer(isEnsembleServer)
                .zookeeperUrl(fabricService.getZookeeperUrl())
                .zookeeperPassword(isEnsembleServer && zookeeperPassword != null ? zookeeperPassword : fabricService.getZookeeperPassword())
                .proxyUri(proxyUri != null ? proxyUri : fabricService.getMavenRepoURI())
                .profiles(getProfileNames())
                .dataStoreProperties(getDataStoreProperties())
                .dataStoreType(dataStoreType != null && isEnsembleServer ? dataStoreType : fabricService.getDataStore().getType());

        if( gearProfile !=null ) {
            builder.gearProfile(gearProfile);
        }

        CreateContainerMetadata[] metadatas = fabricService.createContainers(builder.build());

        if (isEnsembleServer && metadatas != null && metadatas.length > 0 && metadatas[0].isSuccess()) {
            ShellUtils.storeZookeeperPassword(session, metadatas[0].getCreateOptions().getZookeeperPassword());
            if (session != null) {
                // store OpenShift credentials too
                session.put(OPENSHIFT_USER, login);
                session.put(OPENSHIFT_USER_PASSWORD, password);
            }
        }

        // display containers
        displayContainers(metadatas);
        return null;
    }

    @Override
    protected void preCreateContainer(String name) {
        super.preCreateContainer(name);
        // validate number is not out of bounds
        if (number < 0 || number > 99) {
            throw new IllegalArgumentException("The number of containers must be between 1 and 99.");
        }
        if (isEnsembleServer && number > 1) {
            throw new IllegalArgumentException("Can not create a new ZooKeeper ensemble on multiple containers.  Create the containers first and then use the fabric:create command instead.");
        }
    }

    protected void displayContainers(CreateContainerMetadata[] metadatas) {
        List<CreateContainerMetadata> success = new ArrayList<CreateContainerMetadata>();
        List<CreateContainerMetadata> failures = new ArrayList<CreateContainerMetadata>();
        for (CreateContainerMetadata metadata : metadatas) {
            (metadata.isSuccess() ? success : failures).add(metadata);
        }
        if (success.size() > 0) {
            System.out.println("The following containers have been created successfully:");
            for (CreateContainerMetadata m : success) {
                System.out.println("\t" + m.toString());
            }
        }
        if (failures.size() > 0) {
            System.out.println("The following containers have failed:");
            for (CreateContainerMetadata m : failures) {
                System.out.println("\t" + m.getContainerName() + ": " + m.getFailure().getMessage());
            }
        }
    }

}
