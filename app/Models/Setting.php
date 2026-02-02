<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Schema;

class Setting extends Model
{
    use HasFactory;

    protected $fillable = [
        'group',
        'key',
        'value',
    ];

    public static function forGroup(string $group): array
    {
        try {
            if (static::shouldBypassCache()) {
                return static::fetchGroup($group);
            }

            return Cache::rememberForever(static::cacheKey($group), function () use ($group) {
                return static::fetchGroup($group);
            });
        } catch (\Throwable $e) {
            try {
                return static::fetchGroup($group);
            } catch (\Throwable $inner) {
                return [];
            }
        }
    }

    public static function setGroup(string $group, array $values): void
    {
        foreach ($values as $key => $value) {
            static::query()->updateOrCreate(
                ['group' => $group, 'key' => $key],
                ['value' => $value]
            );
        }

        Cache::forget(static::cacheKey($group));
    }

    public static function clearGroup(string $group): void
    {
        static::query()->where('group', $group)->delete();

        Cache::forget(static::cacheKey($group));
    }

    protected static function cacheKey(string $group): string
    {
        return "settings.{$group}";
    }

    protected static function shouldBypassCache(): bool
    {
        $driver = config('cache.default');

        if ($driver !== 'database') {
            return false;
        }

        return ! Schema::hasTable('cache');
    }

    protected static function fetchGroup(string $group): array
    {
        if (! Schema::hasTable('settings')) {
            return [];
        }

        return static::query()
            ->where('group', $group)
            ->pluck('value', 'key')
            ->toArray();
    }
}
