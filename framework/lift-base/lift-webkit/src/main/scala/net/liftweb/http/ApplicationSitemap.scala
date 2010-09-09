package net.liftweb {
package http {
  
  import net.liftweb.util.{Helpers,Props}
  import net.liftweb.common.{Box,Empty,Failure,Full}
  import net.liftweb.sitemap.{SiteMap}
  
  trait SitemapComponent extends { _: EnvironmentComponent with HTTPComponent =>
    object SiteMap {
      
      private var _sitemap: Box[SiteMap] = Empty
      private var sitemapFunc: Box[() => SiteMap] = Empty
      private object sitemapRequestVar extends TransientRequestVar(resolveSitemap())
      
      /**
       * Set to false if you want to have 404's handled the same way in dev and production mode
       */
      @volatile var displayHelpfulSiteMapMessages_? = true
      
      /**
       * The default location to send people if SiteMap access control fails. The path is
       * expressed a a List[String]
       */
      @volatile var siteMapFailRedirectLocation: List[String] = List()
      
      /**
      * Set the sitemap to a function that will be run to generate the sitemap.
      *
      * This allows for changing the SiteMap when in development mode and having
      * the function re-run for each request.
      */
      def setSiteMapFunc(smf: () => SiteMap) {
        sitemapFunc = Full(smf)
        if (!Props.devMode) {
          resolveSitemap()
        }
      }

      /**
      * Define the sitemap.
      */
      def setSiteMap(sm: => SiteMap) {
        this.setSiteMapFunc(() => sm)
      }
      
      private case class PerRequestPF[A, B](f: PartialFunction[A, B]) extends PartialFunction[A, B] {
        def isDefinedAt(a: A) = f.isDefinedAt(a)
        def apply(a: A) = f(a)
      }

      private def resolveSitemap(): Box[SiteMap] = {
        this.synchronized {
          Environment.runAsSafe {
            sitemapFunc.flatMap {
              smf =>
              HTTP.statefulRewrite.remove {
                case PerRequestPF(_) => true
                case _ => false
              }
              val sm = smf()
              _sitemap = Full(sm)
              for (menu <- sm.menus;
                   val loc = menu.loc;
                   rewrite <- loc.rewritePF) HTTP.statefulRewrite.append(PerRequestPF(rewrite))
              _sitemap
            }
          }
        }
      }

      def siteMap: Box[SiteMap] = if (Props.devMode) {
        this.synchronized {
          sitemapRequestVar.is
        }
      } else _sitemap
      
    }
  }
  
}}

