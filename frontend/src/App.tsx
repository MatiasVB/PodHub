import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './app/AuthProvider';
import { ProtectedRoute } from './app/ProtectedRoute';
import { Layout } from './components/Layout';
import { ErrorBoundary } from './components/ErrorBoundary';

// Pages (will be created)
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { PodcastsListPage } from './pages/PodcastsListPage';
import { PodcastDetailPage } from './pages/PodcastDetailPage';
import { EpisodePlayerPage } from './pages/EpisodePlayerPage';
import { UserProfilePage } from './pages/UserProfilePage';
import { UserSubscriptionsPage } from './pages/UserSubscriptionsPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 5 * 60 * 1000, // 5 minutes
    },
  },
});

function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              {/* Public routes */}
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />

            {/* Protected routes with layout */}
            <Route element={<Layout />}>
              <Route
                path="/podcasts"
                element={
                  <ProtectedRoute>
                    <PodcastsListPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/podcasts/:podcastId"
                element={
                  <ProtectedRoute>
                    <PodcastDetailPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/episodes/:episodeId"
                element={
                  <ProtectedRoute>
                    <EpisodePlayerPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/users/:userId"
                element={
                  <ProtectedRoute>
                    <UserProfilePage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/users/:userId/subscriptions"
                element={
                  <ProtectedRoute>
                    <UserSubscriptionsPage />
                  </ProtectedRoute>
                }
              />
            </Route>

            {/* Redirect root to podcasts */}
            <Route path="/" element={<Navigate to="/podcasts" replace />} />
            
            {/* 404 redirect to podcasts */}
            <Route path="*" element={<Navigate to="/podcasts" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
    </ErrorBoundary>
  );
}

export default App;
