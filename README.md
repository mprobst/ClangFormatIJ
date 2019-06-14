# clang-format for IntelliJ based IDEs

ClangFormatIJ hooks up clang-format into IntelliJ based IDEs, such as IntelliJ
IDEA, WebStorm, or similar.

## Installation

To use `clang-format`, please [install it from the JetBrains Marketplace](https://plugins.jetbrains.com/plugin/8396-clangformatij).

Next, you'll need to install `clang-format` itself. To do so, you could grab the binaries...

  - from **npm**: `npm i -g clang-format`
  - from **homebrew** for Mac: `brew install clang-format`
  - by installing the **official llvm toolchain**: http://releases.llvm.org/download.html

Once installed, go into your IntelliJ settings for `clang-format` (Settings > Tools > clang-format) and point the *clang-format binary* field to the executable (e.g. `clang-format.exe` on Windows).

The executable could be located in your `PATH` already, or you could explicitly use something like the following:

  - for **npm/windows**: C:\\Users\\`<your username>`\\AppData\\Roaming\\npm.\node_modules\\clang-format\\bin\\win32\\clang-format.exe
  - for the **llvm** toolchain: `[path/to/clang]`/clang/bin/clang-format
  

## Usage

To use the formatter, simply run the `Reformat Code with clang-format` action inside of your IDE. 

You can also bind the action to a key inside of your settings (Settings > Keymap > Reformat Code with clang-format) and even create a [Macro](https://www.jetbrains.com/help/idea/using-macros-in-the-editor.html) that automatically reformats your code upon saving it.
