<?php

namespace App\Notifications;

use App\Models\Event;
use App\Models\EventComment;
use App\Support\EventMailTemplateManager;
use App\Support\MailConfigManager;
use App\Utils\NotificationUtils;
use App\Utils\UrlUtils;
use Illuminate\Bus\Queueable;
use Illuminate\Notifications\Messages\MailMessage;
use Illuminate\Notifications\Notification;

class EventCommentApprovedNotification extends Notification
{
    use Queueable;

    public function __construct(
        protected EventComment $comment,
        protected Event $event
    ) {
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
        $templates = EventMailTemplateManager::forEvent($this->event);
        $templateKey = $this->templateKey();
        $data = [
            'event_name' => NotificationUtils::eventDisplayName($this->event),
            'comment_author' => $this->comment->author_name,
            'comment_body' => $this->comment->body,
            'photo_url' => $this->comment->photo_url ?? '',
            'event_url' => $this->event->getGuestUrl() ?: UrlUtils::clean($this->event->getEventUrlDomain()),
            'app_name' => config('app.name'),
        ];

        $subject = $templates->renderSubject($templateKey, $data);
        $body = $templates->renderBody($templateKey, $data);

        return (new MailMessage())
            ->subject($subject)
            ->markdown('mail.templates.generic', ['body' => $body]);
    }

    protected function templateKey(): string
    {
        return 'event_comment_approved';
    }
}
