package laika.helium.internal.config

import laika.ast.Path.Root
import laika.ast.*
import laika.helium.config.*
import laika.parse.{ SourceCursor, SourceFragment }

private[helium] sealed trait CommonLayout {
  def defaultBlockSpacing: Length
  def defaultLineHeight: Double
}

private[helium] case class WebLayout(
    contentWidth: Length,
    navigationWidth: Length,
    topBarHeight: Length,
    defaultBlockSpacing: Length,
    defaultLineHeight: Double,
    anchorPlacement: AnchorPlacement
) extends CommonLayout

private[helium] case class WebContent(
    favIcons: Seq[Favicon] = Nil,
    styleIncludes: StyleIncludes = StyleIncludes.empty,
    scriptIncludes: ScriptIncludes = ScriptIncludes.empty,
    topNavigationBar: TopNavigationBar = TopNavigationBar.default,
    mainNavigation: MainNavigation = MainNavigation(),
    pageNavigation: PageNavigation = PageNavigation(),
    footer: Option[TemplateSpan] = Some(HeliumFooter.default),
    landingPage: Option[LandingPage] = None,
    tableOfContent: Option[TableOfContent] = None,
    downloadPage: Option[DownloadPage] = None
)

private[helium] case class PDFLayout(
    pageWidth: Length,
    pageHeight: Length,
    marginTop: Length,
    marginRight: Length,
    marginBottom: Length,
    marginLeft: Length,
    defaultBlockSpacing: Length,
    defaultLineHeight: Double,
    keepTogetherDecoratedLines: Int,
    tableOfContent: Option[TableOfContent] = None
) extends CommonLayout

private[helium] case class EPUBLayout(
    defaultBlockSpacing: Length,
    defaultLineHeight: Double,
    keepTogetherDecoratedLines: Int,
    tableOfContent: Option[TableOfContent] = None
) extends CommonLayout

private[helium] case class TableOfContent(title: String, depth: Int)

private[helium] case class TopNavigationBar(
    homeLink: ThemeLink,
    navLinks: Seq[ThemeLink],
    versionMenu: VersionMenu = VersionMenu.default,
    highContrast: Boolean = false
)

private[helium] object TopNavigationBar {

  def withHomeLink(path: Path): TopNavigationBar =
    TopNavigationBar(IconLink.internal(path, HeliumIcon.home), Nil)

  val default: TopNavigationBar = TopNavigationBar(DynamicHomeLink.default, Nil)
}

private[helium] object HeliumFooter {

  val default: TemplateSpanSequence = TemplateSpanSequence.adapt(
    TemplateString("Site generated by "),
    SpanLink.external("https://typelevel.org/Laika/")("Laika"),
    TemplateString(" with the Helium theme.")
  )

}

private[helium] case class MainNavigation(
    depth: Int = 2,
    includePageSections: Boolean = false,
    prependLinks: Seq[ThemeNavigationSection] = Nil,
    appendLinks: Seq[ThemeNavigationSection] = Nil
)

private[helium] case class PageNavigation(
    enabled: Boolean = true,
    depth: Int = 2,
    sourceBaseURL: Option[String] = None,
    sourceLinkText: String = "Source for this page",
    keepOnSmallScreens: Boolean = false
)

private[helium] case class DownloadPage(
    title: String,
    description: Option[String],
    downloadPath: Path = Root / "downloads",
    includeEPUB: Boolean = true,
    includePDF: Boolean = true
)

private[helium] case class LandingPage(
    logo: Option[Image] = None,
    title: Option[String] = None,
    subtitle: Option[String] = None,
    latestReleases: Seq[ReleaseInfo] = Nil,
    license: Option[String] = None,
    titleLinks: Seq[ThemeLink] = Nil,
    documentationLinks: Seq[TextLink] = Nil,
    projectLinks: Seq[ThemeLinkSpan] = Nil,
    teasers: Seq[Teaser] = Nil
) {

  import LengthUnit._

  val subtitleFontSize: Length =
    if (subtitle.exists(_.length > 75)) px(22)
    else if (subtitle.exists(_.length > 55)) px(27)
    else px(32)

  val teaserTitleFontSize: Length = if (teasers.size <= 4) px(28) else px(20)
  val teaserBodyFontSize: Length  = if (teasers.size <= 4) px(17) else px(15)

}

/** In contrast to the public `LinkGroup` this UI component allows all types of links as children, including menus.
  */
private[helium] case class GenericLinkGroup(links: Seq[ThemeLink], options: Options = Options.empty)
    extends BlockResolver {
  type Self = GenericLinkGroup
  val source: SourceFragment = SourceCursor.Generated

  def resolve(cursor: DocumentCursor): Block = {
    val resolvedLinks = links.map {
      case sr: SpanResolver  => SpanSequence(sr.resolve(cursor))
      case br: BlockResolver => br.resolve(cursor)
    }
    BlockSequence(resolvedLinks, HeliumStyles.linkRow + options)
  }

  def withOptions(newOptions: Options): GenericLinkGroup =
    new GenericLinkGroup(links, newOptions) {}

  def unresolvedMessage: String            = s"Unresolved link group: $this"
  def runsIn(phase: RewritePhase): Boolean = phase.isInstanceOf[RewritePhase.Render]
}

private[helium] object HeliumStyles {
  val row: Options           = Styles("row")
  val linkRow: Options       = Styles("row", "links")
  val buttonLink: Options    = Styles("button-link")
  val textLink: Options      = Styles("text-link")
  val iconLink: Options      = Styles("icon-link")
  val imageLink: Options     = Styles("image-link")
  val menuToggle: Options    = Styles("menu-toggle")
  val menuContainer: Options = Styles("menu-container")
  val menuContent: Options   = Styles("menu-content")
  val versionMenu: Options   = Styles("version-menu")
}

private[helium] case class ThemeFonts(body: String, headlines: String, code: String)

private[helium] case class FontSizes(
    body: Length,
    code: Length,
    title: Length,
    header2: Length,
    header3: Length,
    header4: Length,
    small: Length
)
