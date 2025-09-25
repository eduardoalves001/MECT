% Consider a wireless link between multiple stations for data communications. The bit error rate
% (ber) introduced by the wireless link (due to the variation of the propagation and interference
% factors along with time) is approximately given by the following Markov
% chain: (image on the PDF).
% where the state transition rates are in number of transitions per hour. Consider that the link is
% in an interference state when its ber is at least 10-3 and in a normal state, otherwise. Assume
% that all stations detect with a probability of 100% when the data frames sent by the other stations
% are received with errors. Determine:

% a) the probability of the link being in each of the five states; answer:
% 9.86*10-1 (10-6), 1.31*10-2 (10-5), 6.57*10-4 (10-4), 6.57*10-5 (10-3), 1.31*10-5 (10-2).
fprintf('Task 3\n');
pi0 = 1/(1 + 8/600 + 8/600*5/100 + 8/600*5/100*2/20 + 8/600*5/100*2/20*1/5);
pi1 = pi0*8/600;
pi2 = pi0*8/600*5/100;
pi3 = pi0*8/600*5/100*2/20;
pi4 = pi0*8/600*5/100*2/20*1/5;
p = [pi0, pi1, pi2, pi3, pi4];
fprintf('3.a)\n');
fprintf('1e-6: %.2e; 1e-5: %.2e; 1e-4 %.2e; 1e-3: %.2e; 1e-2: %.2e', p)


% b) the average percentage of time the link is in each of the five states; answer:
% 9.8610-1 (10-6), 1.3110-2 (10-5), 6.5710-4 (10-4), 6.5710-5 (10-3), 1.3110-5 (10-2)
fprintf('\n3.b)\n');
% The probability j gives also the percentage of time that the process is
% in state j.
fprintf('1e-6: %.2e; 1e-5: %.2e; 1e-4 %.2e; 1e-3: %.2e; 1e-2: %.2e', p)

% c) the average ber of the link; answer: 1.3810-6
fprintf('\n3.c)\n');
ber_aux = [1e-6, 1e-5, 1e-4, 1e-3, 1e-2];
ber = sum(p.*ber_aux);
fprintf('ber: %.2e;', ber)

% d) the average holding time (in minutes) of the link in each of the five states;
% answer: 7.5 min (10-6), 0.10 min (10-5), 0.59 min (10-4), 2.86 min (10-3), 12.0 min (10-2)

fprintf('\n3.d)');
t0 = (1/8) * 60;
t1 = (1/(5+600)) * 60;
t2 = (1/(2+100)) * 60;
t3 = (1/(1+20)) * 60;
t4 = (1/5) * 60;
fprintf('\nanswer: %.1f (10^-6), %.2f (10^-5), %.2f (10^-4), %.2f (10^-3), %.1f (10^-2)', t0, t1, t2, t3, t4);

% e) the probability of the link being in the normal state and in interference state; 
% answer: 0.999921 (normal), 7.8910-5 (interference)

% Consider that the link is in an interference state when its ber is at least 10-3 and in a normal state, otherwise.
fprintf('\n3.e)');
pinterf = pi3 + pi4;
pnormal = pi0 + pi1 + pi2;
fprintf('\nanswer: %.6f (normal), %.2e (interference)', pnormal, pinterf);

% f) the average ber of the link when it is in the normal state and when it is in the interference
% state; answer: 1.18*10-6 (normal), 2.50*10-3 (interference)
fprintf('\n3.f)');
bernormal = (pi0*1e-6 + pi1*1e-5 + pi2*1e-4) / pnormal;
berinterf = (pi3*1e-3 + pi4*1e-2) / pinterf;
fprintf('\nanswer: %.2e (normal), %.2e (interference)', bernormal, berinterf);

% g) considering a data frame of size B (in Bytes) sent by one source station to a destination
% station, draw a plot with the same look as the plot below of the probability of the packet
% being received by the destination station with at least one error as a function of the packet
% size (from 64 Bytes up to 1500 Bytes); analyze and justify the results;

x = 64:1500;
perr1 = 1 - (((1 - (1e-6)).^(x.* 8)));
perr2 = 1 - (((1 - (1e-5)).^(x.* 8)));
perr3 = 1 - (((1 - (1e-4)).^(x.* 8)));
perr4 = 1 - (((1 - (1e-3)).^(x.* 8)));
perr5 = 1 - (((1 - (1e-2)).^(x.* 8)));
prob = perr1 * pi0 + perr2 * pi1 + perr3 * pi2 + perr4 * pi3 + perr5 * pi4;
figure(1)
plot(x, prob)
grid
title("Prob. of at least one error");
xlabel("B (Bytes)");
grid on

% h) considering that a data frame of size B (in Bytes) sent by one source station is received
% with at least one error by the destination station, draw a plot with the same look as the
% plot below of the probability of the link being in the normal state as a function of the
% packet size (from 64 Bytes up to 1500 Bytes); analyze and justify the results;

probnormal = (perr1.*pi0 + perr2.*pi1 + perr3.*pi2) ./ (perr1.*pi0 + perr2.*pi1 + perr3.*pi2 + perr4.*pi3 + perr5.*pi4);
figure(2)
plot(x, probnormal)
grid
title("Prob. of Normal State");
xlabel("B (Bytes)");
grid on

% i) considering that a data frame of size B (in Bytes) sent by one source station is received
% without errors by the destination station, draw a plot with the same look as the plot below
% (use the MATLAB function semilogy) of the probability of the link being in the
% interference state as a function of the packet size (from 64 Bytes up to 1500 Bytes);
% analyze and justify the results;

psemerros1 = (1 - 1e-6).^(x.* 8);
psemerros2 = (1 - 1e-5).^(x.* 8);
psemerros3 = (1 - 1e-4).^(x.* 8);
psemerros4 = (1 - 1e-3).^(x.* 8);
psemerros5 = (1 - 1e-2).^(x.* 8);

probinterf = (psemerros4.*pi3 + psemerros5.*pi4)./ (psemerros1.*pi0 + psemerros2.*pi1 + psemerros3.*pi2 + psemerros4.*pi3 + psemerros5.*pi4);

figure(3)
semilogy(x, probinterf)
title("Prob of Intereference State");
xlabel("B (Bytes)");
grid on