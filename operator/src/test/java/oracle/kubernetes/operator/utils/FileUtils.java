// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.
package oracle.kubernetes.operator.utils;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class FileUtils {

  // Note: don't name it 'equalTo' since it conflicts with static importing
  // all the standard matchers, which would force callers to individually import
  // the standard matchers.
  public static FileMatcher fileEqualsTo(File expectedFile) {
    return new FileMatcher(expectedFile);
  }

  public static void assertSameFileContents(Path generated, Path expected) throws Exception {
    assertThat(
        getAssertionReason(generated, expected),
        generated.toFile(),
        fileEqualsTo(expected.toFile()));
  }

  private static String getAssertionReason(Path actualPath, Path expectedPath) {
    return " Contents in actual file "
        + actualPath.toAbsolutePath()
        + " is different from expected file "
        + expectedPath.toAbsolutePath();
  }

  private static class FileMatcher extends TypeSafeDiagnosingMatcher<File> {
    private File expectedFile;

    private FileMatcher(File expectedFile) {
      this.expectedFile = expectedFile;
    }

    String expectedFileLine = null;
    int lineNum = 0;

    @Override
    protected boolean matchesSafely(File actualFile, Description description) {
      if (!fileExists(actualFile, "actual file ", description)
          || !fileExists(expectedFile, "expected file ", description)) {
        return false;
      }
      BufferedReader generatedFileReader = null;
      BufferedReader expectedFileReader = null;
      try {
        generatedFileReader = new BufferedReader(new FileReader(actualFile));
        expectedFileReader = new BufferedReader(new FileReader(expectedFile));
        String actualFileLine;
        expectedFileLine = null;
        lineNum = 0;
        while ((actualFileLine = generatedFileReader.readLine()) != null) {
          lineNum++;
          expectedFileLine = expectedFileReader.readLine();
          if (expectedFileLine == null || !expectedFileLine.trim().equals(actualFileLine.trim())) {
            description.appendText(actualFileLine);
            return false;
          }
        }
        if ((expectedFileLine = expectedFileReader.readLine()) != null) {
          description.appendText("expecting more line(s) from actual file after line " + lineNum);
          return false;
        }
      } catch (IOException e) {
        description.appendText(e.toString());
      } finally {
        close(generatedFileReader);
        close(expectedFileReader);
      }
      return true;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("\n").appendText("Line " + lineNum + ": " + expectedFileLine);
    }

    private void close(Reader reader) {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }

    private boolean fileExists(File file, String fileDesc, Description description) {
      if (!file.exists()) {
        description.appendText(fileDesc + " " + file.getPath() + " does not exist");
        return false;
      }
      return true;
    }
  }
}
