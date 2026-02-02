<?php

namespace Tests\Feature;

use App\Models\Setting;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Cache;
use Tests\TestCase;

class SettingsCacheFallbackTest extends TestCase
{
    use RefreshDatabase;

    public function test_for_group_bypasses_missing_cache_table(): void
    {
        config(['cache.default' => 'database']);
        Cache::clear();

        \Schema::dropIfExists('cache');

        $settings = Setting::forGroup('backups');

        $this->assertIsArray($settings);
        $this->assertSame([], $settings);
    }
}
