<?php

namespace App\Notifications;

use App\Models\Event;
use App\Models\EventComment;
use App\Models\Role;
use App\Support\MailConfigManager;
use App\Utils\NotificationUtils;
use App\Utils\UrlUtils;
use Illuminate\Bus\Queueable;
use Illuminate\Notifications\Messages\MailMessage;
use Illuminate\Notifications\Notification;
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

        return ['mail'];
    }

    public function toMail(object $notifiable): MailMessage
    {
        $eventName = NotificationUtils::eventDisplayName($this->event);
        $eventHash = UrlUtils::encodeId($this->event->id);
        $reviewUrl = route('events.view', ['hash' => $eventHash]);
        $commentPreview = Str::limit($this->comment->body, 160);

        $mail = (new MailMessage())
            ->subject(__('messages.event_comment_pending_subject', ['event' => $eventName]))
            ->line(__('messages.event_comment_pending_intro', ['event' => $eventName]))
            ->line(__('messages.event_comment_pending_from', ['name' => $this->comment->author_name]))
            ->line($commentPreview)
            ->action(__('messages.review_comments'), $reviewUrl);

        if ($this->comment->photo_url) {
            $mail->line(__('messages.event_comment_pending_photo', ['url' => $this->comment->photo_url]));
        }

        return $mail;
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
}
