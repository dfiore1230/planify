<?php

namespace App\Notifications;

use App\Models\Event;
use App\Models\EventComment;
use App\Models\Role;
use App\Support\EventMailTemplateManager;
use App\Support\MailConfigManager;
use App\Utils\NotificationUtils;
use App\Utils\UrlUtils;
use Illuminate\Bus\Queueable;
use Illuminate\Notifications\Messages\MailMessage;
use Illuminate\Notifications\Notification;
use Illuminate\Support\Facades\Route;
use Illuminate\Support\Str;

class EventCommentPendingNotification extends Notification
{
    use Queueable;

    protected EventComment $comment;
    protected Event $event;
    protected ?Role $contextRole;

    public function __construct(EventComment $comment, Event $event, ?Role $contextRole = null)
    {
        $this->comment = $comment;
        $this->event = $event;
        $this->contextRole = $contextRole;
    }

    public function via(object $notifiable): array
    {
        MailConfigManager::applyFromDatabase();

        if (config('mail.disable_delivery')) {
            return [];
        }

        $templates = EventMailTemplateManager::forEvent($this->event);

        return $templates->enabled($this->templateKey()) ? ['mail'] : [];
    }

    public function toMail(object $notifiable): MailMessage
    {
        $eventName = NotificationUtils::eventDisplayName($this->event);
        $eventHash = UrlUtils::encodeId($this->event->id);
        if (Route::has('events.view')) {
            $reviewUrl = route('events.view', ['hash' => $eventHash]);
        } elseif (Route::has('event.view')) {
            $reviewUrl = route('event.view', ['hash' => $eventHash]);
        } else {
            $reviewUrl = url('/events/' . $eventHash . '/view');
        }
        $commentPreview = Str::limit($this->comment->body, 160);
        $templates = EventMailTemplateManager::forEvent($this->event);
        $templateKey = $this->templateKey();
        $data = [
            'event_name' => $eventName,
            'comment_author' => $this->comment->author_name,
            'comment_preview' => $commentPreview,
            'photo_url' => $this->comment->photo_url ?? '',
            'review_url' => $reviewUrl,
            'app_name' => config('app.name'),
        ];

        $subject = $templates->renderSubject($templateKey, $data)
            ?: __('messages.event_comment_pending_subject', ['event' => $eventName]);
        $body = $templates->renderBody($templateKey, $data);

        return (new MailMessage())
            ->subject($subject)
            ->markdown('mail.templates.generic', ['body' => $body]);
    }

    public function toArray(object $notifiable): array
    {
        return [];
    }

    public function toMailHeaders(): array
    {
        $subdomain = $this->contextRole?->subdomain
            ?? $this->event->creatorRole?->subdomain
            ?? $this->event->venue?->subdomain;

        if (! $subdomain) {
            return [];
        }

        return [
            'List-Unsubscribe' => '<' . route('role.unsubscribe', ['subdomain' => $subdomain]) . '>',
            'List-Unsubscribe-Post' => 'List-Unsubscribe=One-Click',
        ];
    }

    protected function templateKey(): string
    {
        return 'event_comment_pending';
    }
}
