package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.SelfCheckApp
import com.example.ui.SelfCheckViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SelfCheck", appName)
  }

  @Test
  fun `verify form inputs and run check navigation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val viewModel = SelfCheckViewModel(context as Application)

    composeTestRule.setContent {
      SelfCheckApp(viewModel = viewModel)
    }

    // Verify Name, Email, Phone inputs and Run Check button exist in the form
    composeTestRule.onNodeWithTag("name_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("email_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("phone_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("run_check_button").performScrollTo().assertIsDisplayed()

    // Enter test values
    composeTestRule.onNodeWithTag("name_input").performTextInput("Jane Doe")
    composeTestRule.onNodeWithTag("email_input").performTextInput("jane@example.com")
    composeTestRule.onNodeWithTag("phone_input").performTextInput("+15551234567")

    // Click 'Run Check' button to navigate to results view
    composeTestRule.onNodeWithTag("run_check_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Verify results view navigation succeeded
    composeTestRule.onNodeWithTag("back_to_form_button").assertExists()
  }

  @Test
  fun `verify data broker report screen and checkboxes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val viewModel = SelfCheckViewModel(context as Application)

    composeTestRule.setContent {
      SelfCheckApp(viewModel = viewModel)
    }

    // Navigate to Brokers Report tab
    composeTestRule.onNodeWithTag("tab_report").performClick()
    composeTestRule.waitForIdle()

    // Verify Spokeo, Whitepages, BeenVerified checkboxes exist
    composeTestRule.onNodeWithTag("checkbox_spokeo").assertExists()
    composeTestRule.onNodeWithTag("checkbox_whitepages").assertExists()
    composeTestRule.onNodeWithTag("checkbox_beenverified").assertExists()

    // Toggle Spokeo checkbox
    composeTestRule.onNodeWithTag("checkbox_spokeo").performClick()
    composeTestRule.waitForIdle()
  }
}

