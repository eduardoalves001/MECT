import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { toast } from 'sonner';

interface CommentFormProps {
  onSubmit: (content: string) => Promise<void>;
  placeholder?: string;
  buttonText?: string;
  isReply?: boolean;
  onCancel?: () => void;
}

export function CommentForm({ 
  onSubmit, 
  placeholder = "Write a comment...", 
  buttonText = "Post Comment",
  isReply = false,
  onCancel
}: CommentFormProps) {
  const [content, setContent] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!content.trim()) {
      toast.error('Comment cannot be empty');
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit(content);
      setContent('');
      toast.success('Comment posted successfully');
    } catch (error) {
      toast.error('Failed to post comment');
      console.error('Error posting comment:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <Textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder={placeholder}
        className="min-h-[100px] resize-none"
        disabled={isSubmitting}
      />
      <div className="flex gap-2 justify-end">
        {isReply && onCancel && (
          <Button
            type="button"
            variant="outline"
            onClick={onCancel}
            disabled={isSubmitting}
          >
            Cancel
          </Button>
        )}
        <Button 
          type="submit" 
          disabled={isSubmitting || !content.trim()}
        >
          {isSubmitting ? 'Posting...' : buttonText}
        </Button>
      </div>
    </form>
  );
}
