<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\URL;

class LogSignedUrlDiagnostics
{
    public function handle(Request $request, Closure $next)
    {
        $signature = $request->query('signature');
        $expires = $request->query('expires');

        $absoluteValid = URL::hasValidSignature($request);
        $relativeValid = URL::hasValidSignature($request, false);

        Log::warning('Signed URL diagnostic', [
            'path' => $request->path(),
            'full_url' => $request->fullUrl(),
            'host' => $request->getHost(),
            'scheme' => $request->getScheme(),
            'relative_valid' => $relativeValid,
            'absolute_valid' => $absoluteValid,
            'expires' => $expires,
            'signature_present' => $signature !== null,
            'forwarded_proto' => $request->header('x-forwarded-proto'),
            'forwarded_host' => $request->header('x-forwarded-host'),
            'forwarded_ssl' => $request->header('x-forwarded-ssl'),
        ]);

        return $next($request);
    }
}
