%Consider a multiple-choice test such that each question has n multiple answers and only one is
%correct. Assume that the student has studied a percentage 𝑝 (with 0% ≤ 𝑝 ≤ 100%) of the test
%content. When a question addresses the content that the student has studied, he selects the right
%answer with 100% of probability. Otherwise, the student always selects randomly one of the n
%answers with a uniform distribution.

% 1a) When 𝑝 = 60% and 𝑛 = 4, determine the probability of the student to select the right
% answer. Answer: 70%
p = 0.6;
n = 4;
% P(E) = P(E|F1)P(F1) + P(E|F2)P(F2)
% = p + (1 – p)/m
pe = p + (1 - p)/n;
fprintf('Answer of 1a: %.0f%%\n', pe*100)

% 1b) When 𝑝 = 70% and 𝑛 = 5, determine the probability of the student to know the answer
% when he selects the right answer. Answer: 92.1%
p = 0.7;
n = 5;
% P(F1|E) = P(E|F1)P(F1) / P(E)
% = p m / [1 + (m – 1) p]
pf1e = p*n/[1+(n-1)*p];
fprintf('Answer of 1b: %.1f%%\n', pf1e*100)

% 1c) Draw a plot (with the same look as the plot below) with the probability of the student to
%select the right answer as a function of the probability 𝑝 (consider the number of multiple
%answers 𝑛 = 3, 4 and 5). What do you conclude from these results?
x = linspace(0,1,100);

% = x + (1 – p)/m

% For n = 3
n = 3;
y3 = x + (1/n * (1-x));
% For n = 4
n = 4;
y4 = x + (1/n * (1-x));
% For n = 5
n = 5;
y5 = x + (1/n * (1-x));
% Generate the figure
figure(1)
plot(x*100, y3*100, 'b:');
hold on;
plot(x*100, y4*100, 'b--');
hold on;
plot(x*100, y5*100, 'b:');
hold off
grid on, ylim([0,100]), title('Title'), xlabel('p%'),ylabel('Probability of the correct answer')
legend('n=3', 'n=4', 'n=5','Location', 'northwest')

% 1d) Draw a plot (with the same look as the plot below) with the probability of the student to
%know the answer when he selects the right answer as a function of the probability 𝑝
%(consider 𝑛 = 3, 4 and 5). What do you conclude from these results?

x = linspace(0,1,100);

% 1 * p / [p + (1 – p)/m]

% For n = 3
n = 3;
y3 = x ./ (x+(1-x)/n);
% For n = 4
n = 4;
y4 = x ./ (x+(1-x)/n);
% For n = 5
n = 5;
y5 = x ./ (x+(1-x)/n);
% Generate the figure
figure(2)
plot(x*100, y3*100, 'b:');
hold on;
plot(x*100, y4*100, 'b--');
hold on;
plot(x*100, y5*100, 'b:');
hold off
grid on, ylim([0,100]), title('Title'), xlabel('p%'),ylabel('Probability of the correct answer')
legend('n=3', 'n=4', 'n=5','Location', 'northwest')