package org.pitest;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.model.Build;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.command.status.StatusScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.mockito.Mock;
import org.pitest.mutationtest.ReportOptions;

public class ScmMojoTest extends BasePitMojoTest {

  private ScmMojo       testee;

  @Mock
  private Build         build;

  @Mock
  private Scm           scm;

  @Mock
  private ScmManager    manager;

  @Mock
  private ScmRepository repository;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.testee = new ScmMojo(this.executionStrategy, this.manager);
    this.testee.setScmRootDir(new File("foo"));
    when(this.project.getBuild()).thenReturn(this.build);
    when(this.build.getSourceDirectory()).thenReturn("foo");
    when(this.build.getOutputDirectory()).thenReturn("foo");
    when(this.project.getScm()).thenReturn(this.scm);
    when(this.manager.makeScmRepository(any(String.class))).thenReturn(
        this.repository);
    configurePitMojo(this.testee, createPomWithConfiguration(""));
  }

  public void testThrowsAnExceptionWhenNoScmConfigured() throws Exception {
    try {
      when(this.project.getScm()).thenReturn(null);
      this.testee.execute();
      fail("Exception expected");
    } catch (final MojoExecutionException ex) {
      assertEquals("No SCM Connection configured.", ex.getMessage());
    }
  }

  public void testUsesCorrectConnectionWhenDeveloperConnectionSet()
      throws Exception {
    final String devUrl = "devcon";
    when(this.scm.getDeveloperConnection()).thenReturn(devUrl);
    setupToReturnNoModifiedFiles();
    this.testee.setConnectionType("developerconnection");
    this.testee.execute();
    verify(this.manager).makeScmRepository(devUrl);

  }
  
  public void testUsesCorrectConnectionWhenNonDeveloperConnectionSet()
      throws Exception {
    final String url = "prodcon";
    when(this.scm.getConnection()).thenReturn(url);
    setupToReturnNoModifiedFiles();
    this.testee.setConnectionType("connection");
    this.testee.execute();
    verify(this.manager).makeScmRepository(url);

  }
  
  public void testClassesAddedToScmAreMutationTested() throws Exception {
    setupConnection();
    when(this.manager.status(any(ScmRepository.class), any(ScmFileSet.class)))
    .thenReturn(new StatusScmResult("", Arrays.asList(new ScmFile("foo/bar/Bar.java",ScmFileStatus.ADDED))));
    this.testee.execute(); 
    verify(this.executionStrategy).execute(any(File.class), any(ReportOptions.class));
  }

  public void testModifiedClassesAreMutationTested() throws Exception {
    setupConnection();
    when(this.manager.status(any(ScmRepository.class), any(ScmFileSet.class)))
    .thenReturn(new StatusScmResult("", Arrays.asList(new ScmFile("foo/bar/Bar.java",ScmFileStatus.MODIFIED))));
    this.testee.execute(); 
    verify(this.executionStrategy).execute(any(File.class), any(ReportOptions.class));
  }
  
  public void testUnknownAndDeletedClassesAreNotMutationTested() throws Exception {
    setupConnection();
    when(this.manager.status(any(ScmRepository.class), any(ScmFileSet.class)))
    .thenReturn(new StatusScmResult("", Arrays.asList(new ScmFile("foo/bar/Bar.java",ScmFileStatus.DELETED), new ScmFile("foo/bar/Bar.java",ScmFileStatus.UNKNOWN))));
    this.testee.execute(); 
    verify(this.executionStrategy, never()).execute(any(File.class), any(ReportOptions.class));
  }
  
  private void setupConnection() {
    when(this.scm.getConnection()).thenReturn("url");
    this.testee.setConnectionType("connection");
  }

  private void setupToReturnNoModifiedFiles() throws ScmException {
    when(this.manager.status(any(ScmRepository.class), any(ScmFileSet.class)))
        .thenReturn(new StatusScmResult("", Collections.<ScmFile> emptyList()));
  }
}