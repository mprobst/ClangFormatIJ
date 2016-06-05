package io.probst.idea.clangformat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class ClangFormatActionTest {

  @Test
 public void testParseReplacementsXml() {
    ClangFormatAction.Replacements replacements = ClangFormatAction.Replacements.parse(
        new ByteArrayInputStream(("<replacements xml:space='preserve' incomplete_format='false'>\n" +
            "<cursor>1</cursor>\n" +
            "<replacement offset='2' length='3'>hello</replacement>\n" +
            "<replacement offset='4' length='5'>&#10;world </replacement>\n" +
            "</replacements>").getBytes(StandardCharsets.UTF_8)));
    assertEquals(1, replacements.cursor);
    assertEquals(2, replacements.replacements.size());
    assertEquals(2, replacements.replacements.get(0).offset);
    assertEquals(3, replacements.replacements.get(0).length);
    assertEquals("hello", replacements.replacements.get(0).value);
    assertEquals(4, replacements.replacements.get(1).offset);
    assertEquals(5, replacements.replacements.get(1).length);
    assertEquals("\nworld ", replacements.replacements.get(1).value);
  }
}