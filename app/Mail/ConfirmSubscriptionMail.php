<?php

namespace App\Mail;

use App\Support\MailTemplateManager;
use Illuminate\Bus\Queueable;
use Illuminate\Mail\Mailable;
use Illuminate\Mail\Mailables\Content;
use Illuminate\Mail\Mailables\Envelope;
use Illuminate\Queue\SerializesModels;

class ConfirmSubscriptionMail extends Mailable
{
    use Queueable, SerializesModels;

    public function __construct(
        public string $confirmUrl,
        public string $listName
    ) {
    }

    public function envelope(): Envelope
    {
        $templates = app(MailTemplateManager::class);
        $data = $this->templateData();

        return new Envelope(subject: $templates->renderSubject('confirm_subscription', $data));
    }

    public function content(): Content
    {
        $templates = app(MailTemplateManager::class);
        $body = $templates->renderBody('confirm_subscription', $this->templateData());

        return new Content(
            markdown: 'mail.templates.generic',
            with: ['body' => $body]
        );
    }

    private function templateData(): array
    {
        return [
            'confirm_url' => $this->confirmUrl,
            'list_name' => $this->listName,
            'app_name' => config('app.name'),
        ];
    }
}
