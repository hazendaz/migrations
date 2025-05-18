/*
 *    Copyright 2010-2025 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.migration.hook;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.migration.MigrationException;
import org.apache.ibatis.migration.VariableReplacer;
import org.apache.ibatis.migration.utils.Util;

public class SqlHookScript implements HookScript {

  protected final File scriptFile;
  protected final String charset;
  protected final Properties variables;
  protected final PrintStream printStream;
  protected final VariableReplacer replacer;

  public SqlHookScript(File scriptFile, String charset, String[] options, Properties variables,
      PrintStream printStream) {
    this.scriptFile = scriptFile;
    this.charset = charset;
    this.variables = variables;
    this.printStream = printStream;
    // options can be local variables in key=value format.
    for (String option : options) {
      int sep = option.indexOf('=');
      if (sep > -1) {
        this.variables.put(option.substring(0, sep), option.substring(sep + 1));
      }
    }
    replacer = new VariableReplacer(this.variables);
  }

  @Override
  public void execute(Map<String, Object> bindingMap) {
    HookContext context = (HookContext) bindingMap.get(MigrationHook.HOOK_CONTEXT);
    printStream.println(Util.horizontalLine("Applying SQL hook: " + scriptFile.getName(), 80));

    try (InputStream inputStream = Files.newInputStream(scriptFile.toPath());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[1024];
      int length;
      while ((length = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, length);
      }
      try (StringReader reader = new StringReader(replacer.replace(outputStream.toString(Charset.forName(charset))))) {
        context.executeSql(reader);
      }
    } catch (IOException e) {
      throw new MigrationException("Error occurred while running SQL hook script.", e);
    }
  }
}
