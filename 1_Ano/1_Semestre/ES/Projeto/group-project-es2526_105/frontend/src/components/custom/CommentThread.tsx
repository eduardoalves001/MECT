import { useState, useEffect } from 'react';
import type { Comment, CommentRequest } from '@/api/types';
import { commentApi } from '@/api';
import { CommentForm } from './CommentForm';
import { CommentItem } from './CommentItem';
import { Separator } from '@/components/ui/separator';
import { useAuth } from '@/contexts/AuthContext';
import { useFeatureFlags } from '@/hooks/useFeatureFlags';
import { toast } from 'sonner';

interface CommentThreadProps {
  vulnerabilityId?: string;
  componentId?: string;
}

export function CommentThread({ vulnerabilityId, componentId }: CommentThreadProps) {
  const [comments, setComments] = useState<Comment[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const { user } = useAuth();
  const { isFeatureEnabled } = useFeatureFlags();

  if (!isFeatureEnabled('comments')) {
    return null;
  }

  useEffect(() => {
    loadComments();
  }, [vulnerabilityId, componentId]);

  const loadComments = async () => {
    if (!vulnerabilityId && !componentId) return;

    setIsLoading(true);
    try {
      const response = vulnerabilityId
        ? await commentApi.getCommentsByVulnerability(vulnerabilityId)
        : await commentApi.getCommentsByComponent(componentId!);

      if (response.success && response.data) {
        setComments(response.data);
      }
    } catch (error) {
      console.error('Error loading comments:', error);
      toast.error('Failed to load comments');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateComment = async (content: string) => {
    const request: CommentRequest = {
      content,
      vulnerabilityId: vulnerabilityId || null,
      componentId: componentId || null,
    };

    const response = await commentApi.createComment(request);
    if (response.success) {
      await loadComments();
    }
  };

  const handleReply = async (parentCommentId: string, content: string) => {
    const request: CommentRequest = {
      content,
      vulnerabilityId: vulnerabilityId || null,
      componentId: componentId || null,
      parentCommentId,
    };

    const response = await commentApi.createComment(request);
    if (response.success) {
      await loadComments();
    }
  };

  const handleDelete = async (commentId: string) => {
    try {
      const response = await commentApi.deleteComment(commentId);
      if (response.success) {
        toast.success('Comment deleted');
        await loadComments();
      }
    } catch (error) {
      toast.error('Failed to delete comment');
      console.error('Error deleting comment:', error);
    }
  };

  if (!vulnerabilityId && !componentId) {
    return null;
  }

  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-lg font-semibold mb-4">Discussion</h3>
        <CommentForm onSubmit={handleCreateComment} />
      </div>

      <Separator />

      <div className="space-y-2">
        {isLoading ? (
          <div className="text-sm text-muted-foreground text-center py-8">
            Loading comments...
          </div>
        ) : comments.length === 0 ? (
          <div className="text-sm text-muted-foreground text-center py-8">
            No comments yet. Be the first to comment!
          </div>
        ) : (
          comments.map((comment) => (
            <CommentItem
              key={comment.id}
              comment={comment}
              onReply={handleReply}
              onDelete={handleDelete}
              currentUserId={user?.username}
            />
          ))
        )}
      </div>
    </div>
  );
}
