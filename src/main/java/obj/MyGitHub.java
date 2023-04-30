package obj;

import helpers.Utils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;

public class MyGitHub {

    private final Credential credential;
    private Git git;

    public Git getGit() {
        return git;
    }

    public MyGitHub(String username, String password) {
        credential = new Credential(username, password);
    }

    public void setGit(String fromFolder) {
        File folder = new File(fromFolder);
        try (Git g = Git.open(folder)) {
            git = g;
            git.pull().setCredentialsProvider(credential).call();
        } catch (IOException | GitAPIException e) {
            git = null;
        }
    }

    public void cloneRepo(String url, String toFolder) {
        File folder = new File(toFolder);
        try (Git cloned = Git.cloneRepository()
                .setURI(url)
                .setDirectory(folder)
                .setCredentialsProvider(credential)
                .call()) {
            git = cloned;
            Utils.printDoneProcess("repo cloned to " + folder.getAbsolutePath());
        } catch (GitAPIException e) {
            git = null;
        }
    }

    public void commitAndPush(String files, String message) {
        try (Git git = this.git) {
            Utils.printProgress("committing");
            git.add().addFilepattern(files).call();
            git.commit().setMessage(message).call();

            Utils.printProgress("Pushing");
            // push to remote:
            git.push()
                    .setCredentialsProvider(credential)
                    .call();
            Utils.printDoneProcess("Pushed");
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    public void commitAndPush(String message) {
        commitAndPush(".", message);
    }

    static class Credential extends UsernamePasswordCredentialsProvider {

        public Credential(String username, String password) {
            super(username, password);
        }
    }
}
