package io.magentys.cinnamon.webdriver.remote

import com.typesafe.config.{Config, ConfigFactory, ConfigObject}
import org.openqa.selenium.remote.DesiredCapabilities

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * Any new 3rd party vendor integration should extend CinnamonRemote.
  * Then the webdriver-factory is responsible to detect which one to use
  * in case more than 1 is required by the user.
  */
trait CinnamonRemote {

  val CAPABILITIES_PROFILES_KEY = "capabilities-profiles"
  val DEFAULTS_SUFFIX = "Defaults"

  val name: String

  def matchesHubUrl(url: String): Boolean

  def capabilities(browserProfile: String, config: Config): DesiredCapabilities = {

    val systemConfig: Config = ConfigFactory.systemProperties
    val defaultConfig = ConfigFactory.load(name + DEFAULTS_SUFFIX)
    val userGlobalConfig = Try(config.getConfig(name)).toOption
    val userProfileConfig = Try(config.getConfig(CAPABILITIES_PROFILES_KEY + "." + browserProfile + "." + name)).toOption

    val allConfig = {
      if (userProfileConfig.isDefined && userGlobalConfig.isDefined) {
        systemConfig.withFallback(userProfileConfig.get).withFallback(userGlobalConfig.get).withFallback(defaultConfig).getConfig(name)
      } else if (userProfileConfig.isDefined) {
        systemConfig.withFallback(userProfileConfig.get).withFallback(defaultConfig).getConfig(name)
      } else if (userGlobalConfig.isDefined) {
        systemConfig.withFallback(userGlobalConfig.get).withFallback(defaultConfig).getConfig(name)
      } else {
        systemConfig.withFallback(defaultConfig).getConfig(name)
      }
    }

    val capsMap: Map[String, AnyRef] = allConfig.entrySet.asScala.map(f => (f.getKey, f.getValue.unwrapped())).toMap
    new DesiredCapabilities(capsMap.asJava)
  }

  def asMap(c: ConfigObject) = {
    c.entrySet.asScala.map(f => (f.getKey, f.getValue.unwrapped())).toMap.asJava
  }
}