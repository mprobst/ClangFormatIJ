package io.probst.idea.clangformat;

import com.google.common.base.Objects;
import com.intellij.ide.util.PropertiesComponent;

public class Settings {
  private static final String CF_BINARY_PROP = ClangFormatConfigurable.class.getName() + ".clangFormatBinary";
  private static final String CF_PATH_PROP = ClangFormatConfigurable.class.getName() + ".path";

  final String clangFormatBinary;
  final String path;

  static Settings get() {
    return new Settings();
  }

  static Settings update(String clangFormatBinary, String path) {
    if ("".equals(clangFormatBinary)) {
      clangFormatBinary = "clang-format";
    }
    if ("".equals(path)) {
      path = null;
    }
    PropertiesComponent props = PropertiesComponent.getInstance();
    props.setValue(CF_BINARY_PROP, clangFormatBinary, "clang-format");
    props.setValue(CF_PATH_PROP, path, null);
    return get();
  }

  private Settings() {
    PropertiesComponent props = PropertiesComponent.getInstance();
    clangFormatBinary = props.getValue(CF_BINARY_PROP, "clang-format");
    path = props.getValue(CF_PATH_PROP);
  }
}
