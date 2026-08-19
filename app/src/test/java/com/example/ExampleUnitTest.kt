package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testUrlParsingAndDomainMatch() {
    val blockedDomains = setOf("instagram.com", "tiktok.com", "reddit.com")
    
    val testUrl1 = "https://www.instagram.com/p/12345"
    val testUrl2 = "https://reddit.com/r/androiddev"
    val testUrl3 = "https://google.com/search?q=kotlin"

    assertTrue(blockedDomains.any { testUrl1.contains(it) })
    assertTrue(blockedDomains.any { testUrl2.contains(it) })
    assertFalse(blockedDomains.any { testUrl3.contains(it) })
  }

  @Test
  fun testSubdomainAndRootMatching() {
    val domain = "facebook.com"
    val rootName = domain.substringBeforeLast(".")
    
    val url1 = "https://m.facebook.com/messages"
    val url2 = "https://touch.facebook.com"
    val url3 = "https://developer.facebook.com"
    val url4 = "https://news.google.com"

    val checkUrl = { url: String ->
      url.contains(domain) || (rootName.length >= 4 && (url.contains("$rootName.") || url.contains("/$rootName") || url.contains(".$rootName")))
    }

    assertTrue(checkUrl(url1))
    assertTrue(checkUrl(url2))
    assertTrue(checkUrl(url3))
    assertFalse(checkUrl(url4))
  }

  @Test
  fun testAdultShieldMatching() {
    val adultDomains = setOf("pornhub.com", "xvideos.com", "onlyfans.com", "chaturbate.com")
    val adultKeywords = setOf("porn", "xxx", "nsfw", "hentai", "camgirl")

    val adultUrl = "https://www.pornhub.com/view_video.php"
    val adultSearch = "watch live camgirl stream"
    val safeSearch = "kotlin android developer guide"

    assertTrue(adultDomains.any { adultUrl.contains(it) })
    assertTrue(adultKeywords.any { adultSearch.contains(it) })
    assertFalse(adultDomains.any { safeSearch.contains(it) })
    assertFalse(adultKeywords.any { safeSearch.contains(it) })
  }

  @Test
  fun testBulkWebsiteParser() {
    val rawInput = "facebook.com, twitter.com; reddit.com\ninstagram.com, youtube.com\n\nnetflix.com"
    val parsed = rawInput.split("\n", ",", ";")
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .distinct()

    assertEquals(6, parsed.size)
    assertTrue(parsed.contains("facebook.com"))
    assertTrue(parsed.contains("netflix.com"))
  }

  @Test
  fun testScheduleDayContains() {
    val workDays = "2,3,4,5,6" // Mon-Fri
    val daysList = workDays.split(",").mapNotNull { it.trim().toIntOrNull() }
    
    assertTrue(daysList.contains(2)) // Mon
    assertTrue(daysList.contains(6)) // Fri
    assertFalse(daysList.contains(1)) // Sun
    assertFalse(daysList.contains(7)) // Sat
  }

  @Test
  fun testInWindowTimeCalculation() {
    // 2:00 PM to 3:00 PM -> 14:00 (840 min) to 15:00 (900 min)
    val startHour = 14
    val startMinute = 0
    val endHour = 15
    val endMinute = 0

    val startMinutes = startHour * 60 + startMinute
    val endMinutes = endHour * 60 + endMinute

    val timeInside = 14 * 60 + 30 // 2:30 PM
    val timeOutside = 15 * 60 + 15 // 3:15 PM

    assertTrue(timeInside in startMinutes until endMinutes)
    assertFalse(timeOutside in startMinutes until endMinutes)
  }

  @Test
  fun testRemainingTimeCalculation() {
    val durationMinutes = 30
    val totalSeconds = durationMinutes * 60L
    assertEquals(1800L, totalSeconds)
  }
}
