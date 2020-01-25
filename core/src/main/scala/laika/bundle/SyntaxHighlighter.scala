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

package laika.bundle

import cats.data.NonEmptyList
import laika.ast.{CodeSpan, CodeSpans}
import laika.parse.Parser
import laika.parse.code.CodeSpanParsers
import laika.parse.code.common.EmbeddedCodeSpans
import laika.parse.text.DelimitedText


trait SyntaxHighlighter {
  
  /** The names of the language (and its optional aliases) as used in text markup */
  def language: NonEmptyList[String]

  /** The parsers for individual code spans written in this language */
  def spanParsers: Seq[CodeSpanParsers]
  
  /** The resulting root parser composed of the individual span parsers to be used in 
    * the parser for the host markup language */
  lazy val rootParser: Parser[Seq[CodeSpan]] =
    EmbeddedCodeSpans.parser(DelimitedText.Undelimited, spanParsers).map(CodeSpans.merge)
  
}

object SyntaxHighlighter {

  /** Creates a syntax highlighter by combining the individual parsers for the various
    * kinds of recognized code spans. This is the default factory for most languages
    * where any unrecognized syntax will simply be put into a top-level default category.
    * 
    * For formats that have stricter rules and require a dedicated, hand-written root 
    * parser, you can use the `apply` method.
    */
  def build (languageName: String, aliases: String*)(parsers: CodeSpanParsers*): SyntaxHighlighter = new SyntaxHighlighter {
    val language = NonEmptyList.of(languageName, aliases:_*)
    val spanParsers = parsers
  }
  
}
