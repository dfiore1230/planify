<?php

namespace App\Http\Controllers;

use App\Models\Event;
use App\Models\EventComment;
use App\Models\Role;
use App\Notifications\EventCommentPendingNotification;
use App\Notifications\EventCommentApprovedNotification;
use App\Notifications\EventCommentSubmittedNotification;
use App\Utils\NotificationUtils;
use App\Utils\UrlUtils;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Notification;

class EventCommentController extends Controller
{
    public function store(Request $request, string $subdomain, string $hash)
    {
        $role = Role::subdomain($subdomain)->firstOrFail();
        $eventId = UrlUtils::decodeId($hash);
        $event = Event::with(['roles', 'venue', 'creatorRole'])->findOrFail($eventId);

        if (! $this->eventMatchesRole($event, $role)) {
            abort(404);
        }

        if ($event->hasPassword()) {
            $sessionKey = 'event_access_' . $event->id;
            $hasAccess = session()->get($sessionKey)
                || ($request->user() && $request->user()->canEditEvent($event));

            if (! $hasAccess) {
                return redirect()->back()->with('error', __('messages.not_authorized'));
            }
        }

        $validated = $request->validate([
            'author_name' => ['required', 'string', 'max:100'],
            'author_email' => ['required', 'email', 'max:255'],
            'body' => ['required', 'string', 'max:2000'],
            'photo_url' => ['nullable', 'url', 'max:2000'],
        ]);

        $user = $request->user();
        $authorEmail = $validated['author_email'];

        $comment = EventComment::create([
            'event_id' => $event->id,
            'author_user_id' => $user?->id,
            'author_name' => $validated['author_name'],
            'author_email' => $authorEmail,
            'body' => $validated['body'],
            'photo_url' => $validated['photo_url'] ?? null,
        ]);

        Notification::route('mail', $comment->author_email)
            ->notify(new EventCommentSubmittedNotification($comment, $event));

        $recipients = NotificationUtils::organizerUsers($event);

        if ($recipients->isNotEmpty()) {
            Notification::send($recipients, new EventCommentPendingNotification($comment, $event, $role));
        }

        return redirect()->back()->with('comment_message', __('messages.comment_submitted_pending'));
    }

    public function approve(Request $request, string $hash, EventComment $comment)
    {
        $eventId = UrlUtils::decodeId($hash);
        $event = Event::with('roles')->findOrFail($eventId);

        if ($comment->event_id !== $event->id) {
            abort(404);
        }

        $user = $request->user();

        if (! $user || (! $user->canEditEvent($event) && ! $user->isAdmin())) {
            return redirect()->back()->with('error', __('messages.not_authorized'));
        }

        if ($comment->approved_at) {
            return redirect()->back()->with('message', __('messages.comment_already_approved'));
        }

        $comment->approved_at = now();
        $comment->approved_by_user_id = $user->id;
        $comment->save();

        Notification::route('mail', $comment->author_email)
            ->notify(new EventCommentApprovedNotification($comment, $event));

        return redirect()->back()->with('message', __('messages.comment_approved'));
    }

    public function reject(Request $request, string $hash, EventComment $comment)
    {
        $eventId = UrlUtils::decodeId($hash);
        $event = Event::with('roles')->findOrFail($eventId);

        if ($comment->event_id !== $event->id) {
            abort(404);
        }

        $user = $request->user();

        if (! $user || (! $user->canEditEvent($event) && ! $user->isAdmin())) {
            return redirect()->back()->with('error', __('messages.not_authorized'));
        }

        $comment->delete();

        return redirect()->back()->with('message', __('messages.comment_rejected'));
    }

    public function destroy(Request $request, string $hash, EventComment $comment)
    {
        $eventId = UrlUtils::decodeId($hash);
        $event = Event::with('roles')->findOrFail($eventId);

        if ($comment->event_id !== $event->id) {
            abort(404);
        }

        $user = $request->user();

        if (! $user || (! $user->canEditEvent($event) && ! $user->isAdmin())) {
            return redirect()->back()->with('error', __('messages.not_authorized'));
        }

        $comment->delete();

        return redirect()->back()->with('message', __('messages.comment_deleted'));
    }

    private function eventMatchesRole(Event $event, Role $role): bool
    {
        if ($event->roles->contains('id', $role->id)) {
            return true;
        }

        if ($event->venue && $event->venue->id === $role->id) {
            return true;
        }

        return $event->creatorRole && $event->creatorRole->id === $role->id;
    }
}
