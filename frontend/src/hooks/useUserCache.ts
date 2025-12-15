import { useQuery } from '@tanstack/react-query';
import { usersAPI } from '../api/endpoints';

export const useUserCache = (userId: string) => {
  return useQuery({
    queryKey: ['user', userId],
    queryFn: async () => {
      const response = await usersAPI.getUser(userId);
      return response.data;
    },
    staleTime: 10 * 60 * 1000, // Cache for 10 minutes
    enabled: !!userId,
  });
};


