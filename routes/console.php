<?php

use Illuminate\Support\Facades\Schedule;
use Illuminate\Support\Facades\Artisan;
use App\Models\Setting;
use App\Services\BackupService;

Schedule::call(function () {
    Artisan::call('app:release-tickets');
})->hourly()->appendOutputTo(storage_path('logs/scheduler.log'));

Schedule::call(function () {
    Artisan::call('app:remind-unpaid-tickets');
})->hourly()->appendOutputTo(storage_path('logs/scheduler.log'));

Schedule::call(function () {
    Artisan::call('app:translate');
})->hourly()->appendOutputTo(storage_path('logs/scheduler.log'));

try {
    $backupSettings = Setting::forGroup('backups');
    $schedule = $backupSettings['schedule'] ?? 'disabled';
    $retentionDays = isset($backupSettings['retention_days']) && $backupSettings['retention_days'] !== ''
        ? (int) $backupSettings['retention_days']
        : null;

    $backupCron = match ($schedule) {
        'daily' => '0 2 * * *',
        'weekly' => '0 2 * * 0',
        'monthly' => '0 2 1 * *',
        default => null,
    };

    if ($backupCron) {
        Schedule::call(function () use ($retentionDays) {
            app(BackupService::class)->createBackup($retentionDays);
        })->cron($backupCron)->timezone(config('app.timezone'))->appendOutputTo(storage_path('logs/scheduler.log'));
    }
} catch (\Throwable $e) {
    // Skip scheduling when settings/cache tables are unavailable during bootstrap.
}
