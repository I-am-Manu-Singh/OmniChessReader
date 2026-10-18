package com.creativeblisslabs.omnimind.core.data.ai

import org.koin.dsl.module
import org.koin.core.module.Module

actual fun platformAiModule(): Module = module {
    // single<AutomationScheduler> { AndroidAutomationScheduler(get()) }
}
