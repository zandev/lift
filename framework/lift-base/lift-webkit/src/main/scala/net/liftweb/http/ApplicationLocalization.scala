package net.liftweb {
package http {
  
  import java.util.{Locale,ResourceBundle,TimeZone}
  import scala.xml.{NodeSeq,Text}
  import net.liftweb.common.{Box,Full,Empty}
  import net.liftweb.util.{Helpers,DateTimeConverter,DefaultDateTimeConverter}
  import net.liftweb.http.provider.HTTPRequest
  
  trait LocalizationComponent { _: Factory =>
    
    type ResourceBundleFactoryPF = PartialFunction[(String, Locale), ResourceBundle]
    
    object Localization {
      
      /**
       * The function referenced here is called if there's a localization lookup failure
       */
      @volatile var localizationLookupFailureNotice: Box[(String, Locale) => Unit] = Empty
      
      /**
       * A function that defines how a String should be converted to XML
       * for the localization stuff.  By default, Text(s) is returned,
       * but you can change this to attempt to parse the XML in the String and
       * return the NodeSeq.
       */
      @volatile var localizeStringToXml: String => NodeSeq = _stringToXml _
      private def _stringToXml(s: String): NodeSeq = Text(s)
      
      val resourceBundleFactories = RulesSeq[ResourceBundleFactoryPF]

      val resourceBundleRequestCalculator = 
        new FactoryMaker[Box[Req] => String](
          (req: Box[Req]) => req.map(_.path.partPath.last).openOr("request")){}
      
      /**
       * The base name of the resource bundle of the lift core code
       */
      @volatile var liftCoreResourceName = "i18n.lift-core"
      
      /**
       * A function that takes the current HTTP request and returns the current
       */
      @volatile var timeZoneCalculator: Box[HTTPRequest] => TimeZone = defaultTimeZoneCalculator _

      def defaultTimeZoneCalculator(request: Box[HTTPRequest]): TimeZone = TimeZone.getDefault
      
      /**
       * A function that takes the current HTTP request and returns the current
       */
      @volatile var localeCalculator: Box[HTTPRequest] => Locale = defaultLocaleCalculator _

      def defaultLocaleCalculator(request: Box[HTTPRequest]) =
        request.flatMap(_.locale).openOr(Locale.getDefault())

      val dateTimeConverter: FactoryMaker[DateTimeConverter] = new FactoryMaker[DateTimeConverter]( () => DefaultDateTimeConverter ) {}
      
    }
  }
  
}}

