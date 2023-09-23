/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package laika.internal.rst.bundle

import laika.api.builder.OperationConfig
import laika.ast.*
import laika.ast.sample.ParagraphCompanionShortcuts
import laika.api.config.Config.ConfigResult
import laika.format.ReStructuredText
import laika.internal.rst.ast.{
  CustomizedTextRole,
  InterpretedText,
  SubstitutionDefinition,
  SubstitutionReference
}
import laika.parse.SourceCursor
import munit.FunSuite

class RewriteRulesSpec extends FunSuite with ParagraphCompanionShortcuts {

  def rewritten(root: RootElement): ConfigResult[RootElement] = {
    val doc = Document(Path.Root, root)
    OperationConfig.default
      .withBundlesFor(ReStructuredText)
      .rewriteRulesFor(doc, RewritePhase.Resolve)
      .flatMap(doc.rewrite)
      .map(_.content)
  }

  def invalidSpan(message: String): InvalidSpan = InvalidSpan(message, SourceCursor.Generated)

  def run(input: RootElement, expected: RootElement)(implicit loc: munit.Location): Unit =
    assertEquals(rewritten(input), Right(expected))

  test("substitutions - replace a single reference with the target span") {
    val input    = RootElement(
      p(SubstitutionReference("id", SourceCursor.Generated)),
      SubstitutionDefinition("id", Text("subst"))
    )
    val expected = RootElement(p("subst"))
    run(input, expected)
  }

  test(
    "substitutions - replace multiple occurrences of the same reference with the same target span"
  ) {
    val input    = RootElement(
      p(
        SubstitutionReference("id", SourceCursor.Generated),
        Text(" foo "),
        SubstitutionReference("id", SourceCursor.Generated)
      ),
      SubstitutionDefinition("id", Text("subst"))
    )
    val expected = RootElement(p(Text("subst"), Text(" foo "), Text("subst")))
    run(input, expected)
  }

  test("substitutions - replace a reference with an unknown substitution id with an invalid span") {
    val input    = RootElement(
      p(SubstitutionReference("id1", SourceCursor.Generated)),
      SubstitutionDefinition("id2", Text("subst"))
    )
    val expected = RootElement(p(invalidSpan("unknown substitution id: id1")))
    run(input, expected)
  }

  test(
    "interpreted text roles - replace a single reference with the result of applying the role function"
  ) {
    val input    = RootElement(
      p(InterpretedText("id", "foo", SourceCursor.Generated)),
      CustomizedTextRole("id", s => Text(s":$s:"))
    )
    val expected = RootElement(p(":foo:"))
    run(input, expected)
  }

  test(
    "interpreted text roles - replace multiple references with the result of applying corresponding role functions"
  ) {
    val input    = RootElement(
      p(
        InterpretedText("id1", "foo", SourceCursor.Generated),
        InterpretedText("id2", "bar", SourceCursor.Generated),
        InterpretedText("id1", "baz", SourceCursor.Generated)
      ),
      CustomizedTextRole("id1", s => Text(":" + s + ":")),
      CustomizedTextRole("id2", s => Text(s".$s."))
    )
    val expected = RootElement(p(Text(":foo:"), Text(".bar."), Text(":baz:")))
    run(input, expected)
  }

  test("interpreted text roles - replace an unknown text role with an invalid span") {
    val input    = RootElement(
      p(InterpretedText("id1", "foo", SourceCursor.Generated)),
      CustomizedTextRole("id2", s => Text(s".$s."))
    )
    val expected = RootElement(p(invalidSpan("unknown text role: id1")))
    run(input, expected)
  }

}
