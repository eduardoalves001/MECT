import { useState } from 'react';
import type { Comment } from '@/api/types';
import { Button } from '@/components/ui/button';
import { Avatar } from '@/components/ui/avatar';
import { CommentForm } from './CommentForm';
import { ChevronDown, ChevronUp } from 'lucide-react';

interface CommentItemProps {
  comment: Comment;
  onReply: (parentCommentId: string, content: string) => Promise<void>;
  onDelete?: (commentId: string) => Promise<void>;
  currentUserId?: string;
  depth?: number;
}

export function CommentItem({ 
  comment, 
  onReply, 
  onDelete,
  currentUserId,
  depth = 0 
}: CommentItemProps) {
  const [showReplyForm, setShowReplyForm] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isCollapsed, setIsCollapsed] = useState(false);
  
  const isAuthor = currentUserId === comment.authorUserId;
  const hasReplies = comment.replies && comment.replies.length > 0;
  const showCollapseButton = depth >= 2 && hasReplies;

  const handleReply = async (content: string) => {
    await onReply(comment.id, content);
    setShowReplyForm(false);
  };

  const handleDelete = async () => {
    if (!window.confirm('Are you sure you want to delete this comment?')) {
      return;
    }
    
    setIsDeleting(true);
    try {
      if (onDelete) {
        await onDelete(comment.id);
      }
    } finally {
      setIsDeleting(false);
    }
  };

  const formatTimeAgo = (date: string) => {
    const now = new Date();
    const commentDate = new Date(date);
    const diffInSeconds = Math.floor((now.getTime() - commentDate.getTime()) / 1000);
    
    if (diffInSeconds < 60) return 'just now';
    if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)} minutes ago`;
    if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)} hours ago`;
    return `${Math.floor(diffInSeconds / 86400)} days ago`;
  };

  const timeAgo = formatTimeAgo(comment.createdAt);

  return (
    <div className={`${depth > 0 ? 'ml-8 mt-4' : 'mt-4'}`}>
      <div className="flex gap-3">
        <Avatar className="h-8 w-8 flex-shrink-0">
          <div className="h-full w-full bg-primary/10 flex items-center justify-center text-sm font-medium">
            {comment.authorUsername.charAt(0).toUpperCase()}
          </div>
        </Avatar>
        
        <div className="flex-1 space-y-2">
          <div className="flex items-center gap-2">
            <span className="font-medium text-sm">{comment.authorUsername}</span>
            <span className="text-xs text-muted-foreground">{timeAgo}</span>
          </div>
          
          <div className="text-sm text-foreground whitespace-pre-wrap">
            {comment.content}
          </div>
          
          <div className="flex gap-2 items-center">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setShowReplyForm(!showReplyForm)}
              className="h-7 px-2 text-xs"
            >
              Reply
            </Button>
            
            {showCollapseButton && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setIsCollapsed(!isCollapsed)}
                className="h-7 px-2 text-xs"
              >
                {isCollapsed ? (
                  <>
                    <ChevronDown className="h-3 w-3 mr-1" />
                    Show {comment.replies?.length} {comment.replies?.length === 1 ? 'reply' : 'replies'}
                  </>
                ) : (
                  <>
                    <ChevronUp className="h-3 w-3 mr-1" />
                    Hide replies
                  </>
                )}
              </Button>
            )}
            
            {isAuthor && onDelete && (
              <Button
                variant="ghost"
                size="sm"
                onClick={handleDelete}
                disabled={isDeleting}
                className="h-7 px-2 text-xs text-destructive hover:text-destructive"
              >
                {isDeleting ? 'Deleting...' : 'Delete'}
              </Button>
            )}
          </div>

          {showReplyForm && (
            <div className="mt-3">
              <CommentForm
                onSubmit={handleReply}
                placeholder={`Reply to ${comment.authorUsername}...`}
                buttonText="Post Reply"
                isReply
                onCancel={() => setShowReplyForm(false)}
              />
            </div>
          )}
        </div>
      </div>

      {hasReplies && !isCollapsed && (
        <div className="space-y-2">
          {comment.replies.map((reply) => (
            <CommentItem
              key={reply.id}
              comment={reply}
              onReply={onReply}
              onDelete={onDelete}
              currentUserId={currentUserId}
              depth={depth + 1}
            />
          ))}
        </div>
      )}
    </div>
  );
}
