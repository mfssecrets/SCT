package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AuthRepository
import com.example.data.UsernameValidationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var context: Context
  private lateinit var repository: AuthRepository

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext<Context>()
    repository = AuthRepository.getInstance(context)
  }

  @Test
  fun `read string from context`() {
    val appName = context.getString(R.string.app_name)
    assertEquals("Clean Shield", appName)
  }

  @Test
  fun `test pin validation rules`() {
    // Weak PINs
    assertNotNull(repository.validatePin("123456")) // Sequential
    assertNotNull(repository.validatePin("654321")) // Sequential descending
    assertNotNull(repository.validatePin("000000")) // All identical
    assertNotNull(repository.validatePin("111111")) // All identical
    assertNotNull(repository.validatePin("12345"))  // Too short
    assertNotNull(repository.validatePin("1234567")) // Too long

    // Valid strong 6-digit PIN
    assertNull(repository.validatePin("492718"))
    assertNull(repository.validatePin("830192"))
  }

  @Test
  fun `test username instagram format rules`() = runBlocking {
    // Invalid formats
    assertTrue(repository.validateUsername("ab") is UsernameValidationResult.Invalid) // < 3 chars
    assertTrue(repository.validateUsername(".startwithdot") is UsernameValidationResult.Invalid)
    assertTrue(repository.validateUsername("endwithdot.") is UsernameValidationResult.Invalid)
    assertTrue(repository.validateUsername("consecutive..dot") is UsernameValidationResult.Invalid)
    assertTrue(repository.validateUsername("has spaces") is UsernameValidationResult.Invalid)
    assertTrue(repository.validateUsername("has@special#chars") is UsernameValidationResult.Invalid)

    // Valid formats
    assertTrue(repository.validateUsername("alex_shield.01") is UsernameValidationResult.Valid)
    assertTrue(repository.validateUsername("john_doe") is UsernameValidationResult.Valid)
    assertTrue(repository.validateUsername("secure.user99") is UsernameValidationResult.Valid)
  }
}
